package com.maskan.mobileapp.data.repository

import com.maskan.mobileapp.data.model.Property
import com.maskan.mobileapp.data.model.PropertyIdCodeEntry
import com.maskan.mobileapp.data.model.PropertyType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Building-level co-ownership cap (feature-properties.md / feature-iap.md). */
const val MAX_CO_OWNERS = 7

/** Free-tier co-owner limit per building — Pro allows up to [MAX_CO_OWNERS] (feature-iap.md). */
const val FREE_CO_OWNER_LIMIT = 1

/** Free-tier owned-building cap (ANDROID_PROPERTIES_FEATURE_SPEC.md §8) — co-owned buildings never count against this. */
const val FREE_PROPERTY_LIMIT = 1

/**
 * §8's lock algorithm, reproduced exactly: a non-Pro owner's oldest
 * [FREE_PROPERTY_LIMIT] buildings (by their oldest flat's `createdAt`) stay
 * fully usable forever; anything created after that is locked whenever the
 * owner isn't currently Pro. A co-owner is never locked by the owner's plan
 * — only the owner's own plan matters when *they* are viewing.
 */
fun isBuildingLocked(buildingKey: String, ownerLandlordId: String, viewerId: String, viewerIsPro: Boolean, allOwnedProperties: List<Property>): Boolean {
    if (viewerIsPro || ownerLandlordId != viewerId) return false
    val orderedKeys = allOwnedProperties
        .filter { it.landlordId == viewerId }
        .groupBy { it.groupKey }
        .entries
        .sortedBy { (_, flats) -> flats.minOf { it.createdAt?.time ?: Long.MAX_VALUE } }
        .map { it.key }
    if (orderedKeys.size <= FREE_PROPERTY_LIMIT) return false
    val unlockedKeys = orderedKeys.take(FREE_PROPERTY_LIMIT)
    return buildingKey !in unlockedKeys
}

data class NewFlatInput(
    val unitName: String,
    val monthlyRent: Double,
    val propertyIdCode: String,
)

data class NewPropertyInput(
    val propertyType: PropertyType,
    val buildingName: String,
    val address: String,
    val flats: List<NewFlatInput>,
    val electricityAccountNumber: String?,
    val houseTaxNumber: String?,
    val waterTaxNumber: String?,
)

data class SharedBuildingEdits(
    val buildingName: String,
    val address: String,
    val electricityAccountNumber: String?,
    val houseTaxNumber: String?,
    val waterTaxNumber: String?,
)

/**
 * One shared instance owned at the landlord tab-shell level (00-overview.md).
 * No individual screen should start/stop this listener.
 */
class PropertyRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) {
    private val propertiesCollection = firestore.collection("properties")
    private val codesCollection = firestore.collection("propertyIdCodes")

    /** Active (non-deleted) properties owned or co-owned by this landlord. */
    private val _properties = MutableStateFlow<List<Property>>(emptyList())
    val properties: StateFlow<List<Property>> = _properties

    /** Soft-deleted (`isDeleted == true`) properties — same source listener as [properties] (feature-properties.md). */
    private val _archivedProperties = MutableStateFlow<List<Property>>(emptyList())
    val archivedProperties: StateFlow<List<Property>> = _archivedProperties

    private var listenerJob: Job? = null

    fun startListening(scope: CoroutineScope, landlordId: String) {
        listenerJob?.cancel()
        listenerJob = propertyFlow(landlordId).onEach { list ->
            _properties.value = list.filterNot { it.isArchived }
            _archivedProperties.value = list.filter { it.isArchived }
        }.launchIn(scope)
    }

    fun stopListening() {
        listenerJob?.cancel()
        listenerJob = null
        _properties.value = emptyList()
        _archivedProperties.value = emptyList()
    }

    /** Merges owner + co-owner matches, deduped by doc id — mirrors TenantRepository's dual-query pattern. */
    private fun propertyFlow(landlordId: String) = combine(
        ownedPropertyFlow(landlordId),
        coOwnedPropertyFlow(landlordId),
    ) { owned, coOwned -> (owned + coOwned).distinctBy { it.id } }

    private fun ownedPropertyFlow(landlordId: String) = callbackFlow {
        val registration = propertiesCollection
            .whereEqualTo("landlordId", landlordId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObjects(Property::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    private fun coOwnedPropertyFlow(landlordId: String) = callbackFlow {
        val registration = propertiesCollection
            .whereArrayContains("coOwners", landlordId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObjects(Property::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    /** Pull-to-refresh: force a server read, bypassing cache; listener stays attached. */
    suspend fun refresh(landlordId: String) {
        val owned = propertiesCollection.whereEqualTo("landlordId", landlordId).get(Source.SERVER).await()
        val coOwned = propertiesCollection.whereArrayContains("coOwners", landlordId).get(Source.SERVER).await()
        val list = (owned.toObjects(Property::class.java) + coOwned.toObjects(Property::class.java)).distinctBy { it.id }
        _properties.value = list.filterNot { it.isArchived }
        _archivedProperties.value = list.filter { it.isArchived }
    }

    suspend fun isCodeAvailable(code: String): Boolean =
        !codesCollection.document(code).get().await().exists()

    /**
     * Currency handling (ANDROID_PROPERTIES_FEATURE_SPEC.md §9): changing
     * currency in Settings must batch-update the `currency` field on every
     * property this landlord *primarily* owns (not co-owned ones, which
     * keep the primary owner's currency) — otherwise tenants, who can't
     * read the `landlords` collection, keep seeing whatever currency their
     * property was stamped with at creation. Covers both active and
     * archived properties. Chunked at 400 writes per batch (Firestore's
     * per-batch cap is 500) since a long-running landlord could plausibly
     * exceed that in one go.
     */
    suspend fun updateCurrencyForOwnedProperties(landlordId: String, currencyCode: String) {
        val owned = propertiesCollection.whereEqualTo("landlordId", landlordId).get().await()
        owned.documents.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc -> batch.update(doc.reference, "currency", currencyCode) }
            batch.commit().await()
        }
    }

    /** One-off fetch. */
    suspend fun getById(propertyId: String): Property? =
        propertiesCollection.document(propertyId).get().await().toObject(Property::class.java)

    /**
     * Fallback lookup for Flutter-era tenant docs that predate
     * `Tenant.propertyDocumentId` (09-tenant-app.md) — prefer reading the
     * property doc ID directly off the tenant when present.
     */
    suspend fun findByPropertyIdCode(propertyIdCode: String): Property? =
        propertiesCollection.whereEqualTo("propertyIdCode", propertyIdCode.uppercase())
            .limit(1)
            .get()
            .await()
            .toObjects(Property::class.java)
            .firstOrNull()

    /**
     * Ad-hoc live listener for the tenant app (no shared session-scoped
     * service exists there — see 09-tenant-app.md), so the tenant's property
     * card reflects photo/occupied changes without a manual refresh. Caller
     * manages its own collection lifecycle.
     */
    fun propertyDocFlow(propertyId: String) = callbackFlow {
        val registration = propertiesCollection.document(propertyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObject(Property::class.java))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Creates one `properties` doc per flat + reserves each code in
     * `propertyIdCodes` in the same batch, so the reservation is atomic with
     * creation (02-data-models.md). Returns the created property IDs (for a
     * follow-up photo upload).
     */
    suspend fun addProperty(landlordId: String, input: NewPropertyInput, currencyCode: String): List<String> {
        val batch = firestore.batch()
        val createdIds = mutableListOf<String>()

        for (flat in input.flats) {
            val propertyRef = propertiesCollection.document()
            createdIds += propertyRef.id

            val doc = hashMapOf(
                "landlordId" to landlordId,
                "name" to input.buildingName,
                "address" to input.address,
                "unit" to flat.unitName,
                "monthlyRent" to flat.monthlyRent,
                "occupied" to false,
                "propertyIdCode" to flat.propertyIdCode,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "propertyType" to input.propertyType.raw,
                "buildingName" to input.buildingName,
                "electricityAccountNumber" to input.electricityAccountNumber,
                "houseTaxNumber" to input.houseTaxNumber,
                "waterTaxNumber" to input.waterTaxNumber,
                "photoUrl" to null,
                "currency" to currencyCode,
            )
            batch.set(propertyRef, doc)

            val codeRef = codesCollection.document(flat.propertyIdCode)
            val codeDoc = hashMapOf(
                "landlordId" to landlordId,
                "propertyId" to propertyRef.id,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
            batch.set(codeRef, codeDoc)
        }

        batch.commit().await()
        return createdIds
    }

    /** Building-level fields apply to every flat sharing the building; unit/rent update only [property]. */
    suspend fun editProperty(property: Property, siblingIds: List<String>, shared: SharedBuildingEdits, unit: String, monthlyRent: Double) {
        val batch = firestore.batch()
        val allIds = (siblingIds + property.id).distinct()
        for (id in allIds) {
            val ref = propertiesCollection.document(id)
            batch.update(
                ref,
                mapOf(
                    "buildingName" to shared.buildingName,
                    "name" to shared.buildingName,
                    "address" to shared.address,
                    "electricityAccountNumber" to shared.electricityAccountNumber,
                    "houseTaxNumber" to shared.houseTaxNumber,
                    "waterTaxNumber" to shared.waterTaxNumber,
                ),
            )
        }
        batch.update(propertiesCollection.document(property.id), mapOf("unit" to unit, "monthlyRent" to monthlyRent))
        batch.commit().await()
    }

    /**
     * Soft delete (feature-properties.md): properties are never hard-deleted.
     * Sets `isDeleted = true` + `deletedAt` on the property doc and
     * `retired = true` + `deletedAt` on its `propertyIdCodes` entry (the code
     * is never reused) — same two fields iOS's `deleteProperty` stamps.
     * Bills and payments are left untouched. Blocked entirely if occupied.
     */
    suspend fun deleteProperty(property: Property) {
        check(!property.occupied) { "Occupied properties can't be deleted." }
        val batch = firestore.batch()
        batch.update(propertiesCollection.document(property.id), deletedFields())
        // set(merge) rather than update(): a batch `update` on a nonexistent doc throws
        // NOT_FOUND and rolls back the *whole* batch, including the isDeleted flip above —
        // this must succeed even for legacy/manually-seeded properties whose propertyIdCodes
        // registry doc was never created.
        batch.set(codesCollection.document(property.propertyIdCode), retiredFields(), com.google.firebase.firestore.SetOptions.merge())
        batch.commit().await()
    }

    private fun deletedFields(): Map<String, Any> = mapOf(
        "isDeleted" to true,
        "deletedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
    )

    private fun retiredFields(): Map<String, Any> = mapOf(
        "retired" to true,
        "deletedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
    )

    /**
     * Add Flat to an existing building (ANDROID_PROPERTIES_FEATURE_SPEC.md
     * §6.3) — only offered when the building's type supports multiple units.
     * Shared building fields are copied from [representative] (an existing
     * sibling flat); `occupied` starts false and the code is reserved in the
     * same batch as creation, same as [addProperty].
     */
    suspend fun addFlat(representative: Property, unitName: String, monthlyRent: Double, propertyIdCode: String): String {
        val propertyRef = propertiesCollection.document()
        val batch = firestore.batch()
        val doc = hashMapOf<String, Any?>(
            "landlordId" to representative.landlordId,
            "name" to representative.name,
            "address" to representative.address,
            "unit" to unitName,
            "monthlyRent" to monthlyRent,
            "occupied" to false,
            "propertyIdCode" to propertyIdCode,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "propertyType" to representative.propertyTypeRaw,
            "buildingName" to representative.buildingName,
            "electricityAccountNumber" to representative.electricityAccountNumber,
            "houseTaxNumber" to representative.houseTaxNumber,
            "waterTaxNumber" to representative.waterTaxNumber,
            "photoUrl" to representative.photoUrl,
            "currency" to representative.currency,
            "coOwners" to representative.coOwners,
        )
        batch.set(propertyRef, doc)

        val codeRef = codesCollection.document(propertyIdCode)
        batch.set(
            codeRef,
            hashMapOf(
                "landlordId" to representative.landlordId,
                "propertyId" to propertyRef.id,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            ),
        )
        batch.commit().await()
        return propertyRef.id
    }

    /**
     * Delete a whole building (ANDROID_PROPERTIES_FEATURE_SPEC.md §6.6):
     * soft-deletes every flat doc and retires every one of their registry
     * codes, in one batch — same semantics as [deleteProperty] applied per
     * flat. Blocked unless every flat is vacant.
     */
    suspend fun deleteBuilding(flats: List<Property>) {
        check(flats.all { !it.occupied }) { "All units must be vacant before deleting the property." }
        val batch = firestore.batch()
        for (flat in flats) {
            batch.update(propertiesCollection.document(flat.id), deletedFields())
            batch.set(codesCollection.document(flat.propertyIdCode), retiredFields(), com.google.firebase.firestore.SetOptions.merge())
        }
        batch.commit().await()
    }

    /**
     * Adds a co-owner (by resolved landlord UID) across every flat sharing
     * [siblingIds] + [property]'s building — co-owners get full management
     * access to the whole building (feature-properties.md). [limit] is
     * [FREE_CO_OWNER_LIMIT] or [MAX_CO_OWNERS] depending on the owner's Pro
     * status (feature-iap.md) — the caller resolves which applies.
     */
    suspend fun addCoOwner(property: Property, siblingIds: List<String>, coOwnerUid: String, limit: Int = MAX_CO_OWNERS) {
        val currentCount = property.coOwners?.size ?: 0
        check(currentCount < limit) { "A building can have at most $limit co-owner${if (limit == 1) "" else "s"} on your current plan." }
        val batch = firestore.batch()
        for (id in (siblingIds + property.id).distinct()) {
            batch.update(propertiesCollection.document(id), "coOwners", com.google.firebase.firestore.FieldValue.arrayUnion(coOwnerUid))
        }
        batch.commit().await()
    }

    /** Removes a co-owner across every flat sharing the building, or lets a co-owner leave. */
    suspend fun removeCoOwner(property: Property, siblingIds: List<String>, coOwnerUid: String) {
        val batch = firestore.batch()
        for (id in (siblingIds + property.id).distinct()) {
            batch.update(propertiesCollection.document(id), "coOwners", com.google.firebase.firestore.FieldValue.arrayRemove(coOwnerUid))
        }
        batch.commit().await()
    }

    /** Two-phase upload: doc(s) already exist with photoUrl=null; compress+upload, then patch. */
    suspend fun uploadPhoto(landlordId: String, propertyIds: List<String>, imageBytes: ByteArray) {
        val ref = storage.reference.child("properties/$landlordId/${UUID.randomUUID()}.jpg")
        ref.putBytes(imageBytes).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        val batch = firestore.batch()
        for (id in propertyIds) {
            batch.update(propertiesCollection.document(id), "photoUrl", downloadUrl)
        }
        batch.commit().await()
    }
}

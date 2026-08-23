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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

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

    private val _properties = MutableStateFlow<List<Property>>(emptyList())
    val properties: StateFlow<List<Property>> = _properties

    private var listenerJob: Job? = null

    fun startListening(scope: CoroutineScope, landlordId: String) {
        listenerJob?.cancel()
        listenerJob = propertyFlow(landlordId).onEach { _properties.value = it }.launchIn(scope)
    }

    fun stopListening() {
        listenerJob?.cancel()
        listenerJob = null
        _properties.value = emptyList()
    }

    private fun propertyFlow(landlordId: String) = callbackFlow {
        val registration = propertiesCollection
            .whereEqualTo("landlordId", landlordId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Property::class.java).orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** Pull-to-refresh: force a server read, bypassing cache; listener stays attached. */
    suspend fun refresh(landlordId: String) {
        val snapshot = propertiesCollection
            .whereEqualTo("landlordId", landlordId)
            .get(Source.SERVER)
            .await()
        _properties.value = snapshot.toObjects(Property::class.java)
    }

    suspend fun isCodeAvailable(code: String): Boolean =
        !codesCollection.document(code).get().await().exists()

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
    suspend fun addProperty(landlordId: String, input: NewPropertyInput): List<String> {
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

    /** Deletion is blocked entirely if the property is currently occupied. */
    suspend fun deleteProperty(property: Property) {
        check(!property.occupied) { "Occupied properties can't be deleted." }
        val batch = firestore.batch()
        batch.delete(propertiesCollection.document(property.id))
        batch.delete(codesCollection.document(property.propertyIdCode))
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

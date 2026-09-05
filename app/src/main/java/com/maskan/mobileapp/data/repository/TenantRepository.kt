package com.maskan.mobileapp.data.repository

import com.maskan.mobileapp.data.model.Property
import com.maskan.mobileapp.data.model.Tenant
import com.maskan.mobileapp.data.util.PasswordHasher
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.util.Date

data class AssignedTenant(val tenant: Tenant, val temporaryPassword: String)

/**
 * One shared instance owned at the landlord tab-shell level. Tenants are
 * queried *by property IDs* (`whereIn`, chunked to 30 — Firestore's cap),
 * so this listener is (re)started whenever the property ID set actually
 * changes, guarded against churn on every unrelated snapshot
 * (00-overview.md / 04-landlord-dashboard.md).
 *
 * Every query also filters on `landlordId`. Firestore rejects a whole list
 * query outright — not per-document — if it can't statically prove the
 * security rule holds for every possible match; the `tenants` rule is an OR
 * of `landlordId == auth.uid` / `auth.uid == tenantId` / property-co-owner
 * access, and a query with no `landlordId` filter gives Firestore nothing to
 * prove the first branch with, so it denies the entire query with
 * PERMISSION_DENIED (silently, from this class's point of view, since errors
 * here just drop the snapshot).
 *
 * Tenants are matched on **either** `propertyId` or `propertyDocumentId`,
 * merged and de-duped client-side: iOS-created tenant docs put the property's
 * human-readable code (not the Firestore doc ID) in `propertyId` and rely on
 * `propertyDocumentId` for the real doc ID, while Android/Flutter-created
 * docs use `propertyId` correctly and never set `propertyDocumentId`. Fixing
 * iOS's `propertyId` value isn't this repo's to do (shared schema/other
 * client — see CLAUDE.md), so both fields are queried here instead.
 */
class TenantRepository(private val firestore: FirebaseFirestore, private val functions: FirebaseFunctions) {
    private val tenantsCollection = firestore.collection("tenants")
    private val propertiesCollection = firestore.collection("properties")

    private val _tenants = MutableStateFlow<List<Tenant>>(emptyList())
    val tenants: StateFlow<List<Tenant>> = _tenants

    private var listenerJobs: List<Job> = emptyList()
    private var currentKey: Pair<String, Set<String>>? = null
    private val perChunkResults = mutableMapOf<Int, List<Tenant>>()

    fun startListening(scope: CoroutineScope, landlordId: String, propertyIds: List<String>) {
        val key = landlordId to propertyIds.toSet()
        if (key == currentKey) return
        currentKey = key

        listenerJobs.forEach { it.cancel() }
        perChunkResults.clear()
        _tenants.value = emptyList()
        if (propertyIds.isEmpty()) return

        listenerJobs = propertyIds.chunked(30).mapIndexed { index, chunk ->
            tenantChunkFlow(landlordId, chunk)
                .onEach { list ->
                    perChunkResults[index] = list
                    _tenants.value = perChunkResults.values.flatten()
                }
                .launchIn(scope)
        }
    }

    fun stopListening() {
        listenerJobs.forEach { it.cancel() }
        listenerJobs = emptyList()
        currentKey = null
        perChunkResults.clear()
        _tenants.value = emptyList()
    }

    private fun tenantChunkFlow(landlordId: String, propertyIds: List<String>) = combine(
        tenantQueryFlow(tenantsCollection.whereEqualTo("landlordId", landlordId).whereIn("propertyId", propertyIds)),
        tenantQueryFlow(tenantsCollection.whereEqualTo("landlordId", landlordId).whereIn("propertyDocumentId", propertyIds)),
    ) { byPropertyId, byPropertyDocumentId ->
        (byPropertyId + byPropertyDocumentId).distinctBy { it.id }
    }

    private fun tenantQueryFlow(query: Query) = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            trySend(snapshot?.toObjects(Tenant::class.java).orEmpty())
        }
        awaitClose { registration.remove() }
    }

    /** Pull-to-refresh: force a server read; listeners stay attached. */
    suspend fun refresh(landlordId: String, propertyIds: List<String>) {
        if (propertyIds.isEmpty()) {
            _tenants.value = emptyList()
            return
        }
        val results = propertyIds.chunked(30).flatMap { chunk ->
            listOf(
                tenantsCollection.whereEqualTo("landlordId", landlordId).whereIn("propertyId", chunk),
                tenantsCollection.whereEqualTo("landlordId", landlordId).whereIn("propertyDocumentId", chunk),
            )
        }.map { it.get(Source.SERVER).await().toObjects(Tenant::class.java) }
        _tenants.value = results.flatten().distinctBy { it.id }
    }

    /**
     * Generates temp password + salt, writes the `tenants` doc (hash, never
     * plaintext) and sets the property's `occupied = true` in one batch. The
     * plaintext password is returned once and never stored (02-data-models.md).
     */
    suspend fun assignTenant(landlordId: String, propertyId: String, name: String, contact: String, moveInDate: Date): AssignedTenant {
        val property = propertiesCollection.document(propertyId).get().await().toObject(Property::class.java)
            ?: error("Property not found")

        val salt = PasswordHasher.generateSalt()
        val tempPassword = PasswordHasher.generateTempPassword()
        val hash = PasswordHasher.hash(salt, tempPassword)

        val tenantRef = tenantsCollection.document()
        val batch = firestore.batch()
        batch.set(
            tenantRef,
            hashMapOf(
                "propertyId" to propertyId,
                "name" to name,
                "contact" to contact,
                "moveInDate" to moveInDate,
                "propertyIdCode" to property.propertyIdCode,
                "passwordHash" to hash,
                "passwordSalt" to salt,
                "createdAt" to FieldValue.serverTimestamp(),
                "landlordId" to landlordId,
                "status" to "active",
            ),
        )
        batch.update(propertiesCollection.document(propertyId), "occupied", true)
        batch.commit().await()

        val tenant = Tenant(
            id = tenantRef.id,
            propertyId = propertyId,
            name = name,
            contact = contact,
            moveInDate = moveInDate,
            propertyIdCode = property.propertyIdCode,
            passwordHash = hash,
            passwordSalt = salt,
            landlordId = landlordId,
            statusRaw = "active",
        )
        return AssignedTenant(tenant, tempPassword)
    }

    suspend fun updateTenant(tenantId: String, name: String, contact: String, moveInDate: Date) {
        tenantsCollection.document(tenantId)
            .update(mapOf("name" to name, "contact" to contact, "moveInDate" to moveInDate))
            .await()
    }

    suspend fun setRentDueDay(tenantId: String, day: Int) {
        tenantsCollection.document(tenantId).update("rentDueDay", day).await()
    }

    /**
     * Landlord-initiated credential reset (06-landlord-tenants.md's only other
     * path to a fresh password was move-out + reassign; this is the direct
     * equivalent without freeing the property). Same one-time-reveal contract
     * as `assignTenant`: the plaintext password is returned once and never
     * stored.
     */
    suspend fun regeneratePassword(tenant: Tenant): String {
        val salt = PasswordHasher.generateSalt()
        val tempPassword = PasswordHasher.generateTempPassword()
        val hash = PasswordHasher.hash(salt, tempPassword)
        tenantsCollection.document(tenant.id)
            .update(mapOf("passwordHash" to hash, "passwordSalt" to salt))
            .await()
        return tempPassword
    }

    /** Revokes login immediately (empty hash can never match) and frees the property, in one batch. */
    suspend fun markMovedOut(tenant: Tenant, moveOutDate: Date) {
        val batch = firestore.batch()
        batch.update(
            tenantsCollection.document(tenant.id),
            mapOf("status" to "old", "moveOutDate" to moveOutDate, "passwordHash" to ""),
        )
        batch.update(propertiesCollection.document(tenant.resolvedPropertyId), "occupied", false)
        batch.commit().await()
    }

    /**
     * Soft delete (feature-tenants.md): sets `status = "deleted"` and clears
     * `passwordHash`, but the document is never removed — payment/bill
     * history for the tenant's property must survive. Only an active
     * tenant's delete also frees the property.
     */
    suspend fun deleteTenant(tenant: Tenant) {
        val batch = firestore.batch()
        batch.update(tenantsCollection.document(tenant.id), mapOf("status" to "deleted", "passwordHash" to ""))
        if (tenant.isActive) {
            batch.update(propertiesCollection.document(tenant.resolvedPropertyId), "occupied", false)
        }
        batch.commit().await()
    }

    /** Tenant-side self lookup: resolve "which tenant am I" by the code they logged in with. */
    suspend fun findByPropertyIdCode(propertyIdCode: String): Tenant? =
        tenantsCollection.whereEqualTo("propertyIdCode", propertyIdCode.uppercase())
            .limit(1)
            .get()
            .await()
            .toObjects(Tenant::class.java)
            .firstOrNull()

    /**
     * Live self-listener for the signed-in tenant. `tenantId` is the
     * Firebase Auth UID, which *equals* the tenant's Firestore doc ID —
     * the `tenantLogin` Cloud Function mints the custom token from the
     * tenant doc ID (09-tenant-app.md).
     */
    fun tenantDocFlow(tenantId: String) = callbackFlow {
        val registration = tenantsCollection.document(tenantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObject(Tenant::class.java))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Current password is verified client-side for fast feedback; the actual
     * Firestore write goes through the `changeTenantPassword` Cloud Function
     * (Admin SDK) because the tenant's custom-token UID isn't permitted to
     * write to the tenants collection directly (security rule requires
     * landlordId == auth.uid).
     */
    suspend fun changePassword(tenant: Tenant, currentPassword: String, newPassword: String) {
        val currentHash = PasswordHasher.hash(tenant.passwordSalt, currentPassword)
        check(currentHash == tenant.passwordHash) { "Current password is incorrect" }

        val newSalt = PasswordHasher.generateSalt()
        val newHash = PasswordHasher.hash(newSalt, newPassword)
        functions.getHttpsCallable("changeTenantPassword")
            .call(hashMapOf("passwordHash" to newHash, "passwordSalt" to newSalt))
            .await()
    }
}

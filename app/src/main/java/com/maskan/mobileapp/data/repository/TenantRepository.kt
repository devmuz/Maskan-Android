package com.maskan.mobileapp.data.repository

import com.maskan.mobileapp.data.model.Property
import com.maskan.mobileapp.data.model.Tenant
import com.maskan.mobileapp.data.util.PasswordHasher
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
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
 */
class TenantRepository(private val firestore: FirebaseFirestore) {
    private val tenantsCollection = firestore.collection("tenants")
    private val propertiesCollection = firestore.collection("properties")

    private val _tenants = MutableStateFlow<List<Tenant>>(emptyList())
    val tenants: StateFlow<List<Tenant>> = _tenants

    private var listenerJobs: List<Job> = emptyList()
    private var currentPropertyIds: Set<String> = emptySet()
    private val perChunkResults = mutableMapOf<Int, List<Tenant>>()

    fun startListening(scope: CoroutineScope, propertyIds: List<String>) {
        val idSet = propertyIds.toSet()
        if (idSet == currentPropertyIds) return
        currentPropertyIds = idSet

        listenerJobs.forEach { it.cancel() }
        perChunkResults.clear()
        _tenants.value = emptyList()
        if (propertyIds.isEmpty()) return

        listenerJobs = propertyIds.chunked(30).mapIndexed { index, chunk ->
            tenantChunkFlow(chunk)
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
        currentPropertyIds = emptySet()
        perChunkResults.clear()
        _tenants.value = emptyList()
    }

    private fun tenantChunkFlow(propertyIds: List<String>) = callbackFlow {
        val registration = tenantsCollection
            .whereIn("propertyId", propertyIds)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObjects(Tenant::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    /** Pull-to-refresh: force a server read; listeners stay attached. */
    suspend fun refresh(propertyIds: List<String>) {
        if (propertyIds.isEmpty()) {
            _tenants.value = emptyList()
            return
        }
        val results = propertyIds.chunked(30).map { chunk ->
            tenantsCollection.whereIn("propertyId", chunk).get(Source.SERVER).await().toObjects(Tenant::class.java)
        }
        _tenants.value = results.flatten()
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

    /** Revokes login immediately (empty hash can never match) and frees the property, in one batch. */
    suspend fun markMovedOut(tenant: Tenant, moveOutDate: Date) {
        val batch = firestore.batch()
        batch.update(
            tenantsCollection.document(tenant.id),
            mapOf("status" to "old", "moveOutDate" to moveOutDate, "passwordHash" to ""),
        )
        batch.update(propertiesCollection.document(tenant.propertyId), "occupied", false)
        batch.commit().await()
    }

    /** Only an active tenant's delete also frees the property. */
    suspend fun deleteTenant(tenant: Tenant) {
        val batch = firestore.batch()
        batch.delete(tenantsCollection.document(tenant.id))
        if (tenant.isActive) {
            batch.update(propertiesCollection.document(tenant.propertyId), "occupied", false)
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
}

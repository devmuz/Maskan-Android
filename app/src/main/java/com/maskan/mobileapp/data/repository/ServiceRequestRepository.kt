package com.maskan.mobileapp.data.repository

import com.maskan.mobileapp.data.model.ServiceRequest
import com.maskan.mobileapp.data.model.ServiceRequestCategory
import com.maskan.mobileapp.data.model.ServiceRequestStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await

/**
 * `serviceRequests` collection (02-data-models.md). Tenant-side surface is
 * the ad-hoc `requestsForTenantFlow` below; the landlord side is a 4th
 * shared, session-scoped service (same lifecycle pattern as Properties/
 * Tenants/Billing — 04-landlord-dashboard.md's "Toolbar: Service Requests
 * inbox").
 */
class ServiceRequestRepository(private val firestore: FirebaseFirestore) {
    private val requestsCollection = firestore.collection("serviceRequests")

    private val _requests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val requests: StateFlow<List<ServiceRequest>> = _requests

    private var listenerJob: Job? = null

    fun startListening(scope: CoroutineScope, landlordId: String) {
        listenerJob?.cancel()
        listenerJob = requestsForLandlordFlow(landlordId).onEach { _requests.value = it }.launchIn(scope)
    }

    fun stopListening() {
        listenerJob?.cancel()
        listenerJob = null
        _requests.value = emptyList()
    }

    private fun requestsForLandlordFlow(landlordId: String) = callbackFlow {
        val registration = requestsCollection
            .whereEqualTo("landlordId", landlordId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObjects(ServiceRequest::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    /**
     * Ad-hoc live listener, started immediately once the tenant's UID is
     * known — doesn't need anything else resolved first (09-tenant-app.md).
     */
    fun requestsForTenantFlow(tenantId: String) = callbackFlow {
        val registration = requestsCollection
            .whereEqualTo("tenantId", tenantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObjects(ServiceRequest::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    /**
     * Landlord-only status transition (pending -> inProgress -> resolved, or
     * straight to resolved). Stamps `resolvedAt` when moving to resolved,
     * matching 02-data-models.md's field.
     */
    suspend fun updateStatus(requestId: String, status: ServiceRequestStatus) {
        val updates = mutableMapOf<String, Any?>("status" to status.raw)
        if (status == ServiceRequestStatus.RESOLVED) {
            updates["resolvedAt"] = FieldValue.serverTimestamp()
        }
        requestsCollection.document(requestId).update(updates).await()
    }

    /**
     * Explicit field map (not the `Codable`/data-class sugar) so a
     * security-rules rejection surfaces as a thrown error instead of failing
     * silently (09-tenant-app.md).
     */
    suspend fun submitRequest(
        tenantId: String,
        propertyId: String,
        landlordId: String,
        title: String,
        description: String,
        category: ServiceRequestCategory,
    ): String {
        val ref = requestsCollection.document()
        ref.set(
            hashMapOf(
                "tenantId" to tenantId,
                "propertyId" to propertyId,
                "landlordId" to landlordId,
                "title" to title,
                "description" to description,
                "category" to category.raw,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        return ref.id
    }
}

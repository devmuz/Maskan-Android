package com.maskan.mobileapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * `serviceRequests` collection — one document per tenant maintenance/service
 * request (02-data-models.md). Fully built on both landlord and tenant
 * sides; status transitions are landlord-only, there is no delete.
 */
data class ServiceRequest(
    @DocumentId
    val id: String = "",
    val tenantId: String = "",
    val propertyId: String = "",
    val landlordId: String = "",
    val title: String = "",
    val description: String = "",
    @get:PropertyName("category") @set:PropertyName("category")
    var categoryRaw: String = ServiceRequestCategory.GENERAL.raw,
    @get:PropertyName("status") @set:PropertyName("status")
    var statusRaw: String = ServiceRequestStatus.PENDING.raw,
    @ServerTimestamp
    val createdAt: Date? = null,
    val resolvedAt: Date? = null,
) {
    // @get:Exclude — see the same fix on Tenant/Bill/Property's raw+computed pairs; without
    // it Firestore's reflection maps e.g. getStatus() to the same default key ("status")
    // that statusRaw's @PropertyName already targets, and throws "Found conflicting getters".
    @get:Exclude
    val category: ServiceRequestCategory
        get() = ServiceRequestCategory.entries.firstOrNull { it.raw == categoryRaw } ?: ServiceRequestCategory.GENERAL

    @get:Exclude
    val status: ServiceRequestStatus
        get() = ServiceRequestStatus.entries.firstOrNull { it.raw == statusRaw } ?: ServiceRequestStatus.PENDING
}

enum class ServiceRequestCategory(val raw: String, val label: String) {
    PLUMBING("plumbing", "Plumbing"),
    ELECTRICAL("electrical", "Electrical"),
    APPLIANCE("appliance", "Appliance"),
    GENERAL("general", "General"),
    OTHER("other", "Other"),
}

enum class ServiceRequestStatus(val raw: String, val label: String) {
    PENDING("pending", "Pending"),
    IN_PROGRESS("inProgress", "In Progress"),
    RESOLVED("resolved", "Resolved"),
}

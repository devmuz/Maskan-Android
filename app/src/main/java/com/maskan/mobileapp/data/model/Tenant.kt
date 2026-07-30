package com.maskan.mobileapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** `tenants` collection — one document per tenant, current or moved-out (02-data-models.md). */
data class Tenant(
    @DocumentId
    val id: String = "",
    val propertyId: String = "",
    val name: String = "",
    val contact: String = "",
    val moveInDate: Date? = null,
    val propertyIdCode: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    val landlordId: String? = null,
    @get:PropertyName("status") @set:PropertyName("status")
    var statusRaw: String? = null,
    val moveOutDate: Date? = null,
    val rentDueDay: Int? = null,
) {
    /** Absent `status` field means active (predates the lifecycle feature). */
    val status: TenantStatus
        get() = TenantStatus.entries.firstOrNull { it.raw == statusRaw } ?: TenantStatus.ACTIVE

    val isActive: Boolean
        get() = status == TenantStatus.ACTIVE
}

enum class TenantStatus(val raw: String) {
    ACTIVE("active"),
    OLD("old"),
}

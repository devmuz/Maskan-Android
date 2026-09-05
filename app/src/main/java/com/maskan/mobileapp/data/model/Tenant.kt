package com.maskan.mobileapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
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
    /**
     * Set on iOS-created tenants for direct property lookup. Absent on
     * Flutter-era docs — fall back to a one-time query by `propertyIdCode`
     * when this is null (09-tenant-app.md).
     */
    val propertyDocumentId: String? = null,
) {
    // @get:Exclude: without it, Firestore's reflection maps getStatus() to the same default
    // key ("status") that getStatusRaw()'s @PropertyName already targets, and throws "Found
    // conflicting getters" the first time a real `tenants` doc is deserialized.
    /** Absent `status` field means active (predates the lifecycle feature). */
    @get:Exclude
    val status: TenantStatus
        get() = TenantStatus.entries.firstOrNull { it.raw == statusRaw } ?: TenantStatus.ACTIVE

    @get:Exclude
    val isActive: Boolean
        get() = status == TenantStatus.ACTIVE

    @get:Exclude
    val isDeleted: Boolean
        get() = status == TenantStatus.DELETED

    /**
     * The `properties` doc ID to actually look up or reference. iOS-created
     * tenant docs put the property's human-readable code (not the Firestore
     * doc ID) in `propertyId`, relying on `propertyDocumentId` for the real
     * doc ID instead; Android/Flutter-created docs use `propertyId` correctly
     * and never set `propertyDocumentId`. Always resolve through this instead
     * of reading `propertyId` directly (data-consistency bug in the shared
     * schema across clients, not fixable from this repo — see CLAUDE.md).
     */
    @get:Exclude
    val resolvedPropertyId: String
        get() = propertyDocumentId?.takeIf { it.isNotBlank() } ?: propertyId
}

enum class TenantStatus(val raw: String) {
    ACTIVE("active"),
    OLD("old"),
    DELETED("deleted"),
}

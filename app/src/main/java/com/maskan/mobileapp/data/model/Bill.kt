package com.maskan.mobileapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** `bills` collection — one document per bill (02-data-models.md). */
data class Bill(
    @DocumentId
    val id: String = "",
    val propertyId: String = "",
    val landlordId: String = "",
    @get:PropertyName("type") @set:PropertyName("type")
    var typeRaw: String = BillType.OTHER.raw,
    val period: String = "",
    val amount: Double = 0.0,
    @get:PropertyName("status") @set:PropertyName("status")
    var statusRaw: String = BillStatus.PENDING.raw,
    val dueDate: Date? = null,
    @get:PropertyName("frequency") @set:PropertyName("frequency")
    var frequencyRaw: String? = null,
    val notes: String? = null,
    val paidAt: Date? = null,
    /** Absent/nil means "tenant" (legacy documents predate this field) — 02-data-models.md. */
    @get:PropertyName("paidBy") @set:PropertyName("paidBy")
    var paidByRaw: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
) {
    // @get:Exclude on every computed property below: Firestore's reflection otherwise maps
    // e.g. getStatus() to the same default key ("status") that getStatusRaw()'s @PropertyName
    // annotation already targets, and throws "Found conflicting getters" the first time a
    // real `bills` doc is deserialized. Apply this to any future raw+computed pair too.
    @get:Exclude
    val type: BillType
        get() = BillType.entries.firstOrNull { it.raw == typeRaw } ?: BillType.OTHER

    @get:Exclude
    val status: BillStatus
        get() = BillStatus.entries.firstOrNull { it.raw == statusRaw } ?: BillStatus.PENDING

    @get:Exclude
    val frequency: Frequency?
        get() = Frequency.entries.firstOrNull { it.raw == frequencyRaw }

    @get:Exclude
    val paidBy: PaidBy
        get() = PaidBy.entries.firstOrNull { it.raw == paidByRaw } ?: PaidBy.TENANT

    /**
     * `status == "overdue"` is not reliable (landlord must manually flip it).
     * Real "is this late" must be computed from the due date (02-data-models.md),
     * excluding `verifying` the same way `paid` is excluded — a bill the tenant
     * has already submitted proof of payment for isn't "late" (09-tenant-app.md).
     */
    @Exclude
    fun isActuallyOverdue(startOfToday: Date): Boolean =
        status != BillStatus.PAID && status != BillStatus.VERIFYING && dueDate != null && dueDate.before(startOfToday)
}

enum class PaidBy(val raw: String, val label: String) {
    TENANT("tenant", "Tenant's Bill"),
    LANDLORD("landlord", "Property Expense"),
}

enum class BillType(val raw: String, val label: String, val defaultFrequency: Frequency) {
    RENT("rent", "Rent", Frequency.MONTHLY),
    ELECTRICITY("electricity", "Electricity", Frequency.MONTHLY),
    WATER("water", "Water Tax", Frequency.ANNUAL),
    HOUSE_TAX("houseTax", "House Tax", Frequency.ANNUAL),
    SEWERAGE("sewerage", "Sewerage Tax", Frequency.ANNUAL),
    MAINTENANCE("maintenance", "Maintenance", Frequency.MONTHLY),
    OTHER("other", "Other", Frequency.MONTHLY),
}

enum class BillStatus(val raw: String, val label: String) {
    PENDING("pending", "Pending"),
    PAID("paid", "Paid"),
    OVERDUE("overdue", "Overdue"),
    /** Tenant submitted proof of payment; awaiting landlord Approve/Reject (07-landlord-bills.md). */
    VERIFYING("verifying", "Verifying"),
}

enum class Frequency(val raw: String, val label: String) {
    MONTHLY("monthly", "Monthly"),
    ANNUAL("annual", "Annual"),
}

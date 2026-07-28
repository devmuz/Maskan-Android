package com.dxbaistudio.maskan.data.model

import com.google.firebase.firestore.DocumentId
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
    @ServerTimestamp
    val createdAt: Date? = null,
) {
    val type: BillType
        get() = BillType.entries.firstOrNull { it.raw == typeRaw } ?: BillType.OTHER

    val status: BillStatus
        get() = BillStatus.entries.firstOrNull { it.raw == statusRaw } ?: BillStatus.PENDING

    val frequency: Frequency?
        get() = Frequency.entries.firstOrNull { it.raw == frequencyRaw }

    /**
     * `status == "overdue"` is not reliable (landlord must manually flip it).
     * Real "is this late" must be computed from the due date (02-data-models.md).
     */
    fun isActuallyOverdue(startOfToday: Date): Boolean =
        status != BillStatus.PAID && dueDate != null && dueDate.before(startOfToday)
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
}

enum class Frequency(val raw: String, val label: String) {
    MONTHLY("monthly", "Monthly"),
    ANNUAL("annual", "Annual"),
}

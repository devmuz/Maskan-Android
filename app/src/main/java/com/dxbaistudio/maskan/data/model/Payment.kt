package com.dxbaistudio.maskan.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

/** `payments` collection — one document per payment recorded against a bill. */
data class Payment(
    @DocumentId
    val id: String = "",
    val billId: String = "",
    val propertyId: String = "",
    val landlordId: String = "",
    val amount: Double = 0.0,
    val paidDate: Date? = null,
    /** Free text on the backend; UI offers a fixed chip set (Cash/Bank Transfer/UPI/Cheque/Other). */
    val method: String = "",
    val notes: String? = null,
)

/** Fixed chip sets offered by the UI — any string is valid on the backend. */
object PaymentMethods {
    val billDetailChips = listOf("Cash", "Bank Transfer", "UPI", "Cheque", "Other")
    val tenantRentChips = listOf("Cash", "Bank Transfer", "UPI", "Other")
}

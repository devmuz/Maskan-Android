package com.maskan.mobileapp.data.repository

import com.maskan.mobileapp.data.model.Bill
import com.maskan.mobileapp.data.model.BillType
import com.maskan.mobileapp.data.model.Frequency
import com.maskan.mobileapp.data.model.PaidBy
import com.maskan.mobileapp.data.model.Payment
import com.maskan.mobileapp.data.util.PeriodFormatter
import com.maskan.mobileapp.data.util.toLocalDate
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
import java.time.LocalDate
import java.util.Date

data class NewBillInput(
    val propertyId: String,
    val type: BillType,
    val frequency: Frequency,
    val paidBy: PaidBy,
    val amount: Double,
    val referenceDate: LocalDate,
    val dueDate: Date,
    val notes: String?,
)

/**
 * One shared instance owned at the landlord tab-shell level (00-overview.md).
 * Exposes both `bills` and `payments` — the dashboard chart, Tenant Detail's
 * property-wide payment history, and Bill Detail's per-bill history all read
 * from the same `payments` stream.
 */
class BillingRepository(private val firestore: FirebaseFirestore) {
    private val billsCollection = firestore.collection("bills")
    private val paymentsCollection = firestore.collection("payments")
    private val notificationsCollection = firestore.collection("notifications")

    private val _bills = MutableStateFlow<List<Bill>>(emptyList())
    val bills: StateFlow<List<Bill>> = _bills

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments

    private var billsJob: Job? = null
    private var paymentsJob: Job? = null

    fun startListening(scope: CoroutineScope, landlordId: String) {
        billsJob?.cancel()
        paymentsJob?.cancel()
        billsJob = billsFlow(landlordId).onEach { _bills.value = it }.launchIn(scope)
        paymentsJob = paymentsFlow(landlordId).onEach { _payments.value = it }.launchIn(scope)
    }

    fun stopListening() {
        billsJob?.cancel(); billsJob = null
        paymentsJob?.cancel(); paymentsJob = null
        _bills.value = emptyList()
        _payments.value = emptyList()
    }

    private fun billsFlow(landlordId: String) = callbackFlow {
        val registration = billsCollection
            .whereEqualTo("landlordId", landlordId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObjects(Bill::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    private fun paymentsFlow(landlordId: String) = callbackFlow {
        val registration = paymentsCollection
            .whereEqualTo("landlordId", landlordId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObjects(Payment::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    suspend fun refresh(landlordId: String) {
        val billsSnapshot = billsCollection.whereEqualTo("landlordId", landlordId).get(Source.SERVER).await()
        _bills.value = billsSnapshot.toObjects(Bill::class.java)
        val paymentsSnapshot = paymentsCollection.whereEqualTo("landlordId", landlordId).get(Source.SERVER).await()
        _payments.value = paymentsSnapshot.toObjects(Payment::class.java)
    }

    suspend fun addBill(landlordId: String, input: NewBillInput) {
        val period = PeriodFormatter.periodFor(input.referenceDate, input.frequency)
        billsCollection.add(
            hashMapOf(
                "propertyId" to input.propertyId,
                "landlordId" to landlordId,
                "type" to input.type.raw,
                "period" to period,
                "amount" to input.amount,
                "status" to "pending",
                "dueDate" to input.dueDate,
                "frequency" to input.frequency.raw,
                "paidBy" to input.paidBy.raw,
                "notes" to input.notes,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** Generic path (Bill Detail): marks a specific, already-existing bill paid. */
    suspend fun recordPayment(bill: Bill, amount: Double, paidDate: Date, method: String, notes: String?) {
        val batch = firestore.batch()
        batch.update(billsCollection.document(bill.id), mapOf("status" to "paid", "paidAt" to paidDate))
        batch.set(
            paymentsCollection.document(),
            hashMapOf(
                "billId" to bill.id,
                "propertyId" to bill.propertyId,
                "landlordId" to bill.landlordId,
                "amount" to amount,
                "paidDate" to paidDate,
                "method" to method,
                "notes" to notes,
            ),
        )
        batch.commit().await()
    }

    /**
     * "Record Payment" flow (Dashboard quick action + Tenant Detail): finds-or-creates
     * the bill for this property/type/period (period derived from [paidDate] and the
     * type's default frequency) and marks it paid, in the same batch as the payment doc.
     */
    suspend fun recordPaymentForBillType(landlordId: String, propertyId: String, type: BillType, amount: Double, paidDate: Date, method: String, notes: String?) {
        val period = PeriodFormatter.periodFor(paidDate.toLocalDate(), type.defaultFrequency)
        val existing = billsCollection
            .whereEqualTo("propertyId", propertyId)
            .whereEqualTo("type", type.raw)
            .whereEqualTo("period", period)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()

        val batch = firestore.batch()
        val billRef = existing?.reference ?: billsCollection.document()
        if (existing != null) {
            batch.update(billRef, mapOf("status" to "paid", "paidAt" to paidDate, "amount" to amount))
        } else {
            batch.set(
                billRef,
                hashMapOf(
                    "propertyId" to propertyId,
                    "landlordId" to landlordId,
                    "type" to type.raw,
                    "period" to period,
                    "amount" to amount,
                    "status" to "paid",
                    "dueDate" to paidDate,
                    "frequency" to type.defaultFrequency.raw,
                    "paidAt" to paidDate,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
        }
        batch.set(
            paymentsCollection.document(),
            hashMapOf(
                "billId" to billRef.id,
                "propertyId" to propertyId,
                "landlordId" to landlordId,
                "amount" to amount,
                "paidDate" to paidDate,
                "method" to method,
                "notes" to notes,
            ),
        )
        batch.commit().await()
    }

    /** Overrides this month's pending rent amount without a full payment; creates the bill if absent. */
    suspend fun setPendingAmount(landlordId: String, propertyId: String, amount: Double, dueDate: Date) {
        val period = PeriodFormatter.currentMonthRentPeriod()
        val existing = billsCollection
            .whereEqualTo("propertyId", propertyId)
            .whereEqualTo("type", BillType.RENT.raw)
            .whereEqualTo("period", period)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()

        if (existing != null) {
            existing.reference.update("amount", amount).await()
        } else {
            billsCollection.add(
                hashMapOf(
                    "propertyId" to propertyId,
                    "landlordId" to landlordId,
                    "type" to BillType.RENT.raw,
                    "period" to period,
                    "amount" to amount,
                    "status" to "pending",
                    "dueDate" to dueDate,
                    "frequency" to Frequency.MONTHLY.raw,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    suspend fun markOverdue(billId: String) {
        billsCollection.document(billId).update("status", "overdue").await()
    }

    suspend fun markPending(billId: String) {
        billsCollection.document(billId).update("status", "pending").await()
    }

    suspend fun deleteBill(billId: String) {
        billsCollection.document(billId).delete().await()
    }

    /**
     * Ad-hoc listener for the tenant app (no shared session-scoped service
     * exists there yet — see 09-tenant-app.md). Caller manages its own
     * collection lifecycle (e.g. `collectAsStateWithLifecycle`).
     */
    fun billsForPropertyFlow(propertyId: String) = callbackFlow {
        val registration = billsCollection
            .whereEqualTo("propertyId", propertyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObjects(Bill::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    fun paymentsForPropertyFlow(propertyId: String) = callbackFlow {
        val registration = paymentsCollection
            .whereEqualTo("propertyId", propertyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObjects(Payment::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    /**
     * Landlord confirms a tenant's "I've Paid This Bill" submission
     * (07-landlord-bills.md): the bill -> "paid", `paidAt` taken from the
     * pending payment's `paidDate`. The payment doc itself needs no
     * further write — it was already correctly shaped when the tenant
     * created it.
     */
    suspend fun approvePayment(bill: Bill) {
        val pending = paymentsCollection.whereEqualTo("billId", bill.id).get().await()
            .toObjects(Payment::class.java)
            .maxByOrNull { it.paidDate ?: Date(0) }
            ?: error("No pending payment found for this bill")
        billsCollection.document(bill.id).update(mapOf("status" to "paid", "paidAt" to pending.paidDate)).await()
    }

    /** Deletes the pending payment doc and reverts the bill -> "pending", as if the tenant's submission never happened. */
    suspend fun rejectPayment(bill: Bill) {
        val pendingDoc = paymentsCollection.whereEqualTo("billId", bill.id).get().await().documents
            .maxByOrNull { it.getDate("paidDate")?.time ?: 0L }
        val batch = firestore.batch()
        pendingDoc?.let { batch.delete(it.reference) }
        batch.update(billsCollection.document(bill.id), "status", "pending")
        batch.commit().await()
    }

    /**
     * Tenant-initiated "I've Paid This Bill" (09-tenant-app.md): a batch write
     * that sets the bill to `"verifying"` (plus the amount the tenant entered)
     * and creates a pending `payments` doc. The bill stays `verifying` until
     * the landlord approves (-> paid) or rejects (-> back to pending, payment
     * doc deleted) — see `07-landlord-bills.md`.
     */
    suspend fun submitPaymentForVerification(bill: Bill, amount: Double, paidDate: Date, method: String, notes: String?) {
        val batch = firestore.batch()
        batch.update(billsCollection.document(bill.id), mapOf("status" to "verifying", "amount" to amount))
        batch.set(
            paymentsCollection.document(),
            hashMapOf(
                "billId" to bill.id,
                "propertyId" to bill.propertyId,
                "landlordId" to bill.landlordId,
                "amount" to amount,
                "paidDate" to paidDate,
                "method" to method,
                "notes" to notes,
            ),
        )
        batch.commit().await()
    }

    suspend fun sendReminder(tenantId: String, message: String) {
        notificationsCollection.add(
            hashMapOf(
                "tenantId" to tenantId,
                "type" to "reminder",
                "message" to message,
                "sentAt" to FieldValue.serverTimestamp(),
                "read" to false,
            ),
        ).await()
    }
}

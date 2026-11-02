package com.maskan.mobileapp.ui.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maskan.mobileapp.data.model.Bill
import com.maskan.mobileapp.data.model.BillStatus
import com.maskan.mobileapp.data.model.BillType
import com.maskan.mobileapp.data.model.Frequency
import com.maskan.mobileapp.data.model.PaidBy
import com.maskan.mobileapp.data.model.Payment
import com.maskan.mobileapp.data.model.Property
import com.maskan.mobileapp.data.model.ServiceRequest
import com.maskan.mobileapp.data.model.ServiceRequestCategory
import com.maskan.mobileapp.data.model.ServiceRequestStatus
import com.maskan.mobileapp.data.model.Tenant
import com.maskan.mobileapp.data.util.PeriodFormatter
import com.maskan.mobileapp.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

/**
 * The tenant app's single shared, session-scoped data service — the Android
 * equivalent of iOS's `TenantDataService` (09-tenant-app.md). Created once
 * when the tenant shell mounts (there's no tab bar to survive tab-switches,
 * but the same "one instance per session" rule from 00-overview.md applies).
 *
 * Identity is resolved from the signed-in Firebase Auth UID directly — the
 * `tenantLogin` Cloud Function mints the custom token from the tenant's own
 * Firestore doc ID, so `auth.currentUser.uid == tenants/{id}`. No lookup by
 * Property ID code is needed for this (that cached code from RolePreferences
 * predates this and is unused here now).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TenantSessionViewModel(private val container: AppContainer) : ViewModel() {
    private val uid: String? = container.authRepository.currentUser?.uid

    private val _tenant = MutableStateFlow<Tenant?>(null)
    val tenant: StateFlow<Tenant?> = _tenant

    // Only updated when the resolved property doc id actually changes, so the
    // property/bills/payments listeners below don't restart on every tenant
    // snapshot (09-tenant-app.md's "re-attach ... guarding against restarting
    // on every snapshot if it hasn't [changed]").
    private val _resolvedPropertyId = MutableStateFlow<String?>(null)

    val property: StateFlow<Property?> = _resolvedPropertyId
        .flatMapLatest { id -> if (id == null) flowOf(null) else container.propertyRepository.propertyDocFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Set when the bills listener fails (e.g. a permission error); Home shows "Couldn't load bills". */
    private val _billsError = MutableStateFlow<String?>(null)
    val billsError: StateFlow<String?> = _billsError

    /** Bills visible to the tenant exclude the landlord's own property expenses (09-tenant-app.md). */
    val bills: StateFlow<List<Bill>> = _resolvedPropertyId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else container.billingRepository.billsForPropertyFlow(id) { _billsError.value = it }
        }
        .map { list -> list.filter { it.paidBy != PaidBy.LANDLORD } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = _resolvedPropertyId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else container.billingRepository.paymentsForPropertyFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * This month's rent when the landlord hasn't logged a rent bill yet. The
     * landlord's Tenant Detail already shows the monthly rent as pending in
     * that case, so the tenant sees the same amount here (02-data-models.md →
     * "Tenant-side synthesized current-month rent"). It isn't in Firestore
     * until the tenant reports paying it — see [submitPaymentForVerification].
     */
    val upcomingRentBill: StateFlow<Bill?> = combine(bills, _tenant, property) { list, t, p ->
        val period = PeriodFormatter.currentMonthRentPeriod()
        if (t == null || !t.isActive || p == null || p.monthlyRent <= 0.0) return@combine null
        if (list.any { it.type == BillType.RENT && it.period == period }) return@combine null
        Bill(
            id = UPCOMING_RENT_BILL_ID,
            propertyId = p.id,
            landlordId = p.landlordId,
            typeRaw = BillType.RENT.raw,
            period = period,
            amount = p.monthlyRent,
            statusRaw = BillStatus.PENDING.raw,
            dueDate = PeriodFormatter.rentDueDate(t.rentDueDay),
            frequencyRaw = Frequency.MONTHLY.raw,
            paidByRaw = PaidBy.TENANT.raw,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pendingBills: StateFlow<List<Bill>> = combine(bills, upcomingRentBill) { list, upcoming ->
        (list.filter { it.status != BillStatus.PAID } + listOfNotNull(upcoming)).sortedBy { it.dueDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paidBills: StateFlow<List<Bill>> = bills
        .map { list -> list.filter { it.status == BillStatus.PAID }.sortedByDescending { it.paidAt ?: it.dueDate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDue: StateFlow<Double> = pendingBills
        .map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Falls back to "USD" for properties created before the currency field shipped. */
    val currencyCode: StateFlow<String> = property
        .map { it?.currency ?: "USD" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    // Optimistic local inserts so a just-submitted request shows immediately,
    // before the snapshot listener round-trips; deduped by doc ID once it does.
    private val _optimisticRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())

    private val liveRequests: StateFlow<List<ServiceRequest>> =
        (uid?.let { container.serviceRequestRepository.requestsForTenantFlow(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val requests: StateFlow<List<ServiceRequest>> = combine(liveRequests, _optimisticRequests) { live, optimistic ->
        val liveIds = live.map { it.id }.toSet()
        (optimistic.filterNot { it.id in liveIds } + live).sortedByDescending { it.createdAt ?: Date(0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // The serviceRequests listener above starts immediately, independent
        // of tenant-doc resolution. The tenant doc listener starts next; once
        // it resolves, it drives which property doc the other three listeners
        // attach to (09-tenant-app.md's "listener startup order").
        if (uid != null) {
            viewModelScope.launch {
                container.tenantRepository.saveFcmTokenIfAvailable()
            }
            viewModelScope.launch {
                container.tenantRepository.tenantDocFlow(uid).collect { resolved ->
                    _tenant.value = resolved
                    val propertyId = resolved?.propertyDocumentId?.takeIf { it.isNotBlank() }
                        ?: resolved?.propertyIdCode?.takeIf { it.isNotBlank() }
                            ?.let { code -> container.propertyRepository.findByPropertyIdCode(code)?.id }
                    if (propertyId != _resolvedPropertyId.value) {
                        _resolvedPropertyId.value = propertyId
                    }
                }
            }
        }
    }

    suspend fun submitRequest(title: String, description: String, category: ServiceRequestCategory) {
        val t = _tenant.value ?: error("Tenant not resolved yet")
        val propertyId = _resolvedPropertyId.value ?: error("Property not resolved yet")
        val landlordId = property.value?.landlordId ?: t.landlordId.orEmpty()

        val id = container.serviceRequestRepository.submitRequest(
            tenantId = t.id,
            propertyId = propertyId,
            landlordId = landlordId,
            title = title,
            description = description,
            category = category,
        )
        _optimisticRequests.value = _optimisticRequests.value + ServiceRequest(
            id = id,
            tenantId = t.id,
            propertyId = propertyId,
            landlordId = landlordId,
            title = title,
            description = description,
            categoryRaw = category.raw,
            statusRaw = ServiceRequestStatus.PENDING.raw,
            createdAt = Date(),
        )
    }

    suspend fun submitPaymentForVerification(bill: Bill, amount: Double, paidDate: Date, method: String, notes: String?) {
        container.billingRepository.submitPaymentForVerification(
            bill, amount, paidDate, method, notes,
            createBill = bill.id == UPCOMING_RENT_BILL_ID,
        )
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val t = _tenant.value ?: error("Tenant not resolved yet")
        container.tenantRepository.changePassword(t, currentPassword, newPassword)
    }

    fun signOut() {
        container.authRepository.signOut()
    }

    companion object {
        /** Route/lookup ID for [upcomingRentBill], which has no Firestore doc ID. */
        const val UPCOMING_RENT_BILL_ID = "upcoming-rent"
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TenantSessionViewModel(container) as T
    }
}

/**
 * Resolves a bill route ID against the tenant's bills. The synthesized rent
 * bill's ID resolves to it while it exists, then to the real rent bill for the
 * same period once the tenant's submission has created it — so Bill Detail
 * flips to "Verifying…" instead of "Bill not found".
 */
fun resolveTenantBill(billId: String, bills: List<Bill>, upcomingRent: Bill?): Bill? {
    if (billId != TenantSessionViewModel.UPCOMING_RENT_BILL_ID) return bills.find { it.id == billId }
    return upcomingRent ?: bills.find { it.type == BillType.RENT && it.period == PeriodFormatter.currentMonthRentPeriod() }
}

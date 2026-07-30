package com.maskan.mobileapp.ui.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maskan.mobileapp.data.model.Bill
import com.maskan.mobileapp.data.model.Payment
import com.maskan.mobileapp.data.model.Property
import com.maskan.mobileapp.data.model.Tenant
import com.maskan.mobileapp.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tenant shell session. Unlike the landlord side, no shared tenant-facing
 * data services exist yet in the source app (09-tenant-app.md) — this is
 * new scope, designed to mirror the landlord pattern: one instance, created
 * once per session, owned above the 4 tabs.
 *
 * The tenant's Firebase Auth UID isn't linked back to a `tenants` doc by any
 * custom claim, so identity is resolved from the Property ID code the
 * tenant logged in with (cached locally — see RolePreferences).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TenantSessionViewModel(private val container: AppContainer) : ViewModel() {
    private val _tenant = MutableStateFlow<Tenant?>(null)
    val tenant: StateFlow<Tenant?> = _tenant

    private val _property = MutableStateFlow<Property?>(null)
    val property: StateFlow<Property?> = _property

    val bills: StateFlow<List<Bill>> = _tenant
        .flatMapLatest { t ->
            if (t == null) flowOf(emptyList()) else container.billingRepository.billsForPropertyFlow(t.propertyId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = _tenant
        .flatMapLatest { t ->
            if (t == null) flowOf(emptyList()) else container.billingRepository.paymentsForPropertyFlow(t.propertyId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            container.rolePreferences.tenantPropertyIdCodeFlow.collect { code ->
                if (code != null && _tenant.value == null) {
                    resolveSelf(code)
                }
            }
        }
    }

    private suspend fun resolveSelf(code: String) {
        val resolved = container.tenantRepository.findByPropertyIdCode(code)
        _tenant.value = resolved
        _property.value = resolved?.let { container.propertyRepository.getById(it.propertyId) }
    }

    fun signOut() {
        container.authRepository.signOut()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TenantSessionViewModel(container) as T
    }
}

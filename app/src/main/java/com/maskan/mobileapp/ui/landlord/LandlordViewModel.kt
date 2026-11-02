package com.maskan.mobileapp.ui.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maskan.mobileapp.data.model.Bill
import com.maskan.mobileapp.data.model.Payment
import com.maskan.mobileapp.data.model.Property
import com.maskan.mobileapp.data.model.Tenant
import com.maskan.mobileapp.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * One instance per landlord session, scoped to the "landlord_shell" nav
 * back-stack entry: created once right after login, cleared on sign-out.
 * Owns the three shared data services (Properties/Tenants/Billing) so no
 * individual tab screen starts/stops its own Firestore listener
 * (00-overview.md).
 */
class LandlordViewModel(private val container: AppContainer) : ViewModel() {
    private val landlordId: String
        get() = container.authRepository.currentUser?.uid.orEmpty()

    val properties get() = container.propertyRepository.properties
    val archivedProperties get() = container.propertyRepository.archivedProperties
    val propertiesLoaded get() = container.propertyRepository.hasLoaded
    val tenants get() = container.tenantRepository.tenants
    val bills get() = container.billingRepository.bills
    val payments get() = container.billingRepository.payments
    val requests get() = container.serviceRequestRepository.requests
    val landlord get() = container.landlordRepository.landlord

    val propertyRepository get() = container.propertyRepository
    val tenantRepository get() = container.tenantRepository
    val billingRepository get() = container.billingRepository
    val serviceRequestRepository get() = container.serviceRequestRepository
    val landlordRepository get() = container.landlordRepository
    val purchaseRepository get() = container.purchaseRepository

    val landlordUid: String get() = landlordId

    // Tracks which property docs have a photo upload in flight, so the
    // Properties list can show "Uploading photo…" per building group
    // (05-landlord-properties.md's two-phase create-then-patch flow).
    private val _uploadingPhotoPropertyIds = MutableStateFlow<Set<String>>(emptySet())
    val uploadingPhotoPropertyIds: StateFlow<Set<String>> = _uploadingPhotoPropertyIds

    fun markUploadingPhoto(propertyIds: List<String>) {
        _uploadingPhotoPropertyIds.value = _uploadingPhotoPropertyIds.value + propertyIds
    }

    fun clearUploadingPhoto(propertyIds: List<String>) {
        _uploadingPhotoPropertyIds.value = _uploadingPhotoPropertyIds.value - propertyIds.toSet()
    }

    // Set when the background photo upload after Add Property fails; the
    // Properties list shows it as an alert (iOS `photoUploadError`).
    private val _photoUploadError = MutableStateFlow<String?>(null)
    val photoUploadError: StateFlow<String?> = _photoUploadError

    fun reportPhotoUploadError(message: String?) {
        _photoUploadError.value = message
    }

    init {
        val id = landlordId
        container.propertyRepository.startListening(viewModelScope, id)
        container.billingRepository.startListening(viewModelScope, id)
        container.serviceRequestRepository.startListening(viewModelScope, id)
        container.landlordRepository.startListening(viewModelScope, id)
        // Archived (soft-deleted) properties are included so tenants who moved out
        // of a since-deleted property still show under Tenants → Old, as on iOS.
        viewModelScope.launch {
            combine(container.propertyRepository.properties, container.propertyRepository.archivedProperties) { active, archived ->
                (active + archived).map { it.id }.distinct()
            }.collect { propertyIds ->
                container.tenantRepository.startListening(viewModelScope, id, propertyIds)
            }
        }
        viewModelScope.launch {
            val providerId = container.authRepository.currentUser?.providerData?.firstOrNull()?.providerId
            val loginWith = if (providerId == "google.com") "google" else "email"
            container.landlordRepository.ensureProfileExists(id, container.authRepository.currentUser?.email, loginWith)
            container.landlordRepository.saveFcmTokenIfAvailable(id)
        }
        container.purchaseRepository.activeLandlordId = id
        viewModelScope.launch {
            container.purchaseRepository.loadProducts()
            container.purchaseRepository.restorePurchases()
        }
    }

    /** Pull-to-refresh: force a server read on all three, concurrently; listeners stay attached. */
    suspend fun refreshAll() {
        val id = landlordId
        kotlinx.coroutines.coroutineScope {
            launch { container.propertyRepository.refresh(id) }
            launch { container.billingRepository.refresh(id) }
            launch {
                val repo = container.propertyRepository
                container.tenantRepository.refresh(id, (repo.properties.value + repo.archivedProperties.value).map { it.id }.distinct())
            }
        }
    }

    fun signOut() {
        container.propertyRepository.stopListening()
        container.tenantRepository.stopListening()
        container.billingRepository.stopListening()
        container.serviceRequestRepository.stopListening()
        container.landlordRepository.stopListening()
        container.purchaseRepository.activeLandlordId = null
        container.authRepository.signOut()
    }

    override fun onCleared() {
        container.propertyRepository.stopListening()
        container.tenantRepository.stopListening()
        container.billingRepository.stopListening()
        container.serviceRequestRepository.stopListening()
        container.landlordRepository.stopListening()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LandlordViewModel(container) as T
    }
}

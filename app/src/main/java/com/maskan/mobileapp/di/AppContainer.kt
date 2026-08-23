package com.maskan.mobileapp.di

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.maskan.mobileapp.data.prefs.RolePreferences
import com.maskan.mobileapp.data.prefs.ThemePreferences
import com.maskan.mobileapp.data.repository.AuthRepository
import com.maskan.mobileapp.data.repository.BillingRepository
import com.maskan.mobileapp.data.repository.LandlordRepository
import com.maskan.mobileapp.data.repository.PropertyRepository
import com.maskan.mobileapp.data.repository.PurchaseRepository
import com.maskan.mobileapp.data.repository.ServiceRequestRepository
import com.maskan.mobileapp.data.repository.TenantRepository
import com.maskan.mobileapp.data.repository.UpdateRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage

/**
 * Manual composition root. Everything here is a process-wide singleton,
 * matching the "one shared instance per data domain" rule from
 * 00-overview.md — repositories are created once and injected down, never
 * re-created per screen.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    val rolePreferences = RolePreferences(appContext)
    val themePreferences = ThemePreferences(appContext)
    val authRepository = AuthRepository(firebaseAuth, functions)
    val propertyRepository = PropertyRepository(firestore, storage)
    val tenantRepository = TenantRepository(firestore)
    val billingRepository = BillingRepository(firestore)
    val serviceRequestRepository = ServiceRequestRepository(firestore)
    val landlordRepository = LandlordRepository(firestore)
    val purchaseRepository = PurchaseRepository(appContext, landlordRepository)
    val updateRepository = UpdateRepository(remoteConfig, appContext.packageName, currentVersionCode())

    private fun currentVersionCode(): Long = runCatching {
        val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        PackageInfoCompat.getLongVersionCode(packageInfo)
    }.getOrDefault(0L)
}

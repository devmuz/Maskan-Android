package com.dxbaistudio.maskan.di

import android.content.Context
import com.dxbaistudio.maskan.data.prefs.RolePreferences
import com.dxbaistudio.maskan.data.repository.AuthRepository
import com.dxbaistudio.maskan.data.repository.BillingRepository
import com.dxbaistudio.maskan.data.repository.PropertyRepository
import com.dxbaistudio.maskan.data.repository.TenantRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
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

    val rolePreferences = RolePreferences(appContext)
    val authRepository = AuthRepository(firebaseAuth, functions)
    val propertyRepository = PropertyRepository(firestore, storage)
    val tenantRepository = TenantRepository(firestore)
    val billingRepository = BillingRepository(firestore)
}

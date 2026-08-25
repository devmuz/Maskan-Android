package com.maskan.mobileapp

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.maskan.mobileapp.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MaskanApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(!isDebuggable)

        // Tag crash reports with who was signed in and which role they were using,
        // since a tenant vs. landlord crash usually points at different code paths.
        appScope.launch {
            container.authRepository.authStateFlow.collect { user ->
                crashlytics.setUserId(user?.uid.orEmpty())
            }
        }
        appScope.launch {
            container.rolePreferences.roleFlow.collect { role ->
                crashlytics.setCustomKey("role", role?.raw.orEmpty())
            }
        }
    }
}

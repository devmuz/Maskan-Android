package com.maskan.mobileapp.data.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

/** Result of a launch-time version check against Remote Config. */
data class UpdateStatus(
    val currentVersionCode: Long,
    val latestVersionCode: Long,
    val minSupportedVersionCode: Long,
    val updateUrl: String,
) {
    val isUpdateRequired: Boolean get() = minSupportedVersionCode > currentVersionCode
    val isUpdateAvailable: Boolean get() = latestVersionCode > currentVersionCode
}

/**
 * Checks Firebase Remote Config on app launch for a newer published version.
 * Requires no auth (Remote Config fetches are anonymous), so it can run before
 * sign-in on the splash/role-select screen too.
 *
 * Console setup (same Firebase project as iOS/Flutter): create these Remote
 * Config parameters —
 *  - android_latest_version_code (number): current Play Store versionCode.
 *  - android_min_supported_version_code (number): oldest versionCode still
 *    allowed to run; anything below this forces the update (no dismiss).
 *  - android_update_url (string, optional): defaults to the Play Store listing
 *    for this applicationId if left blank.
 */
class UpdateRepository(
    private val remoteConfig: FirebaseRemoteConfig,
    private val packageName: String,
    private val currentVersionCode: Long,
) {
    init {
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(MINIMUM_FETCH_INTERVAL_SECONDS)
                .build(),
        )
        remoteConfig.setDefaultsAsync(
            mapOf(
                LATEST_VERSION_CODE_KEY to 0L,
                MIN_SUPPORTED_VERSION_CODE_KEY to 0L,
                UPDATE_URL_KEY to "",
            ),
        )
    }

    /** Returns null on fetch failure (offline, etc.) — callers should treat that as "no update". */
    suspend fun checkForUpdate(): UpdateStatus? = runCatching {
        remoteConfig.fetchAndActivate().await()
        val updateUrl = remoteConfig.getString(UPDATE_URL_KEY).ifBlank { "market://details?id=$packageName" }
        UpdateStatus(
            currentVersionCode = currentVersionCode,
            latestVersionCode = remoteConfig.getLong(LATEST_VERSION_CODE_KEY),
            minSupportedVersionCode = remoteConfig.getLong(MIN_SUPPORTED_VERSION_CODE_KEY),
            updateUrl = updateUrl,
        )
    }.getOrNull()

    companion object {
        private const val MINIMUM_FETCH_INTERVAL_SECONDS = 3600L
        private const val LATEST_VERSION_CODE_KEY = "android_latest_version_code"
        private const val MIN_SUPPORTED_VERSION_CODE_KEY = "android_min_supported_version_code"
        private const val UPDATE_URL_KEY = "android_update_url"
    }
}

package com.maskan.mobileapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Whether the landlord onboarding coachmark (point-at-Add-button + property-type intro) has been shown. */
class CoachmarkPreferences(private val context: Context) {
    private val seenKey = booleanPreferencesKey("coachmark_onboarding_seen")

    val hasSeenFlow: Flow<Boolean> = context.maskanDataStore.data.map { it[seenKey] ?: false }

    suspend fun markSeen() {
        context.maskanDataStore.edit { it[seenKey] = true }
    }
}

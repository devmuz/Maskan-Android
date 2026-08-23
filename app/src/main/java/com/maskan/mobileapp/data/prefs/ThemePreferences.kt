package com.maskan.mobileapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** System/Light/Dark, key "maskan.appTheme" — shared between landlord and tenant (08-landlord-settings.md). */
enum class AppTheme(val raw: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromRaw(raw: String?): AppTheme = entries.firstOrNull { it.raw == raw } ?: SYSTEM
    }
}

class ThemePreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("maskan.appTheme")

    val themeFlow: Flow<AppTheme> = context.maskanDataStore.data.map { prefs ->
        AppTheme.fromRaw(prefs[themeKey])
    }

    suspend fun setTheme(theme: AppTheme) {
        context.maskanDataStore.edit { it[themeKey] = theme.raw }
    }
}

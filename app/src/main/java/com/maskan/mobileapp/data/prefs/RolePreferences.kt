package com.maskan.mobileapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Which flow the user entered through — not derived from any Firebase Auth claim (00-overview.md). */
enum class UserRole(val raw: String) {
    LANDLORD("landlord"),
    TENANT("tenant"),
    ;

    companion object {
        fun fromRaw(raw: String?): UserRole? = entries.firstOrNull { it.raw == raw }
    }
}

/** Persists the signed-in role locally (SharedPreferences/DataStore per 00-overview.md). */
class RolePreferences(private val context: Context) {
    private val roleKey = stringPreferencesKey("role")

    // Firebase Auth persists the tenant's custom-token session across relaunches on its own,
    // but nothing links that Firebase Auth UID back to a `tenants` doc (see 09-tenant-app.md).
    // We cache the property-ID-code the tenant logged in with so the app can re-resolve
    // "which tenant am I" via a Firestore lookup without asking them to log in again.
    private val tenantPropertyIdCodeKey = stringPreferencesKey("tenant_property_id_code")

    val roleFlow: Flow<UserRole?> = context.maskanDataStore.data.map { prefs ->
        UserRole.fromRaw(prefs[roleKey])
    }

    val tenantPropertyIdCodeFlow: Flow<String?> = context.maskanDataStore.data.map { prefs ->
        prefs[tenantPropertyIdCodeKey]
    }

    suspend fun setRole(role: UserRole) {
        context.maskanDataStore.edit { it[roleKey] = role.raw }
    }

    suspend fun setTenantPropertyIdCode(code: String) {
        context.maskanDataStore.edit { it[tenantPropertyIdCodeKey] = code }
    }

    suspend fun clearRole() {
        context.maskanDataStore.edit {
            it.remove(roleKey)
            it.remove(tenantPropertyIdCodeKey)
        }
    }
}

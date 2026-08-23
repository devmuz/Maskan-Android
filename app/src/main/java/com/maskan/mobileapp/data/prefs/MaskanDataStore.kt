package com.maskan.mobileapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single shared DataStore instance backing every *Preferences class in this
 * package. DataStore throws if two `preferencesDataStore` delegates target
 * the same file name from the same process, so this must be the one place
 * that declares it — don't redeclare `by preferencesDataStore("maskan_prefs")`
 * elsewhere.
 */
internal val Context.maskanDataStore by preferencesDataStore(name = "maskan_prefs")

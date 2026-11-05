package com.maskan.mobileapp.data.util

import com.maskan.mobileapp.BuildConfig

/**
 * Folder inserted after the top-level Storage prefix (`properties/prod/…`,
 * `tenants/prod/…`) so every upload is grouped by environment: "dev", "uat"
 * or "prod", from the flavor's Firestore database ID. Must match iOS
 * AppEnvironment.storageFolder and the deleteAccount function.
 */
object StorageEnv {
    val folder: String = BuildConfig.FIRESTORE_DATABASE_ID.removePrefix("maskan-")
}

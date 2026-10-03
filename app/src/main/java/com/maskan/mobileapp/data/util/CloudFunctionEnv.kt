package com.maskan.mobileapp.data.util

import com.maskan.mobileapp.BuildConfig

/**
 * Cloud Functions have no per-environment deploy target the way Firestore
 * rules/Storage do (ENVIRONMENTS.md) — environment-aware functions are
 * deployed as one export per database, suffixed by environment (e.g.
 * `tenantLoginProd`). Derived from the same flavor-set
 * `FIRESTORE_DATABASE_ID` ("maskan-dev"/"maskan-uat"/"maskan-prod") other
 * repositories already key off of, so it stays in sync by construction.
 */
object CloudFunctionEnv {
    val suffix: String =
        BuildConfig.FIRESTORE_DATABASE_ID.removePrefix("maskan-").replaceFirstChar(Char::uppercase)
}

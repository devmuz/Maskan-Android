package com.maskan.mobileapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.util.Date

/**
 * `landlords` collection — one document per landlord, document ID == Firebase
 * Auth UID (02-data-models.md). Holds profile fields plus subscription/Pro
 * state.
 */
data class Landlord(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val currencyCode: String? = null,
    /** Admin-granted Pro, never expires. Set manually in the Firebase console. */
    val proOverride: Boolean? = null,
    /** Cross-platform Pro expiry — written by whichever platform confirmed the purchase. */
    val proExpiryDate: Date? = null,
    /** Which platform's purchase this expiry came from, e.g. "ios" / "android". */
    val proSource: String? = null,
) {
    /**
     * Pro status resolution order (02-data-models.md): proOverride first (no
     * expiry check), then proExpiryDate > now as the fallback that lets the
     * *other* platform's purchase be recognized here too.
     */
    @get:Exclude
    val isPro: Boolean
        get() = proOverride == true || (proExpiryDate?.after(Date()) == true)
}

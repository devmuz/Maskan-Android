package com.maskan.mobileapp.data.repository

import com.google.firebase.firestore.SetOptions
import com.maskan.mobileapp.data.model.Landlord
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.maskan.mobileapp.data.util.CloudFunctionEnv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale


/**
 * `landlords` collection — one shared instance owned at the landlord
 * tab-shell level (00-overview.md), same lifecycle pattern as the other
 * three services. Nothing in the app read this doc before now; profile
 * fields (currency, Pro state) all live here.
 */
class LandlordRepository(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
) {
    private val landlordsCollection = firestore.collection("landlords")

    private val _landlord = MutableStateFlow<Landlord?>(null)
    val landlord: StateFlow<Landlord?> = _landlord

    private var listenerJob: Job? = null

    fun startListening(scope: CoroutineScope, landlordId: String) {
        listenerJob?.cancel()
        listenerJob = landlordFlow(landlordId).onEach { _landlord.value = it }.launchIn(scope)
    }

    fun stopListening() {
        listenerJob?.cancel()
        listenerJob = null
        _landlord.value = null
    }

    private fun landlordFlow(landlordId: String) = callbackFlow {
        val registration = landlordsCollection.document(landlordId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObject(Landlord::class.java))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Self-heals a missing profile doc on first landlord screen load, mirroring
     * the existing iOS/Flutter behavior (Maskan_Project_Plan.md). Currency
     * defaults to a best-guess from the device's region, falling back to USD
     * (02-data-models.md).
     */
    suspend fun ensureProfileExists(landlordId: String, email: String?, loginWith: String) {
        val doc = landlordsCollection.document(landlordId).get().await()
        if (doc.exists()) return
        val guessedCurrency = runCatching { Locale.getDefault().let { java.util.Currency.getInstance(it).currencyCode } }
            .getOrDefault("USD")
        landlordsCollection.document(landlordId).set(
            hashMapOf(
                "email" to email.orEmpty(),
                "currency" to guessedCurrency,
                "currencyCode" to guessedCurrency,
                "loginWith" to loginWith,
            ),
        ).await()
    }

    suspend fun setCurrencyCode(landlordId: String, currencyCode: String) {
        // Both names: iOS reads `currency`, older Android builds read `currencyCode`.
        landlordsCollection.document(landlordId).update(mapOf("currency" to currencyCode, "currencyCode" to currencyCode)).await()
    }

    /**
     * Registers this device for push (feature_push.md). Unlike iOS's APNs
     * dance, a token is available immediately from Firebase Messaging without
     * waiting for a system permission prompt, so this can run right at
     * session start rather than needing a "cached until sign-in" fallback.
     * `onNewToken` in [com.maskan.mobileapp.data.push.MaskanMessagingService]
     * covers token refreshes after this point.
     */
    suspend fun saveFcmTokenIfAvailable(landlordId: String) {
        val token = runCatching { com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        landlordsCollection.document(landlordId).set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    /**
     * Co-owner lookup by email (feature-properties.md's `addCoOwner` flow).
     * Relies on the `landlords` collection's open read rule (any authenticated
     * user can read, not just self) — see feature-properties.md's "Co-owner
     * lookup fix". The prospective co-owner must already have a Maskan account.
     */
    /**
     * Landlord profiles are private to their owner (Firestore rules), so
     * co-owner lookups run server-side: [findLandlordUidByEmail] resolves an
     * invite email, [landlordEmails] returns the owner's and co-owners' emails
     * for a building the caller owns or co-owns.
     */
    suspend fun findLandlordUidByEmail(email: String): String? = try {
        val result = functions.getHttpsCallable("findLandlordByEmail${CloudFunctionEnv.suffix}")
            .call(hashMapOf("email" to email.trim().lowercase(Locale.ROOT)))
            .await()
        (result.getData() as? Map<*, *>)?.get("uid") as? String
    } catch (e: FirebaseFunctionsException) {
        if (e.code == FirebaseFunctionsException.Code.NOT_FOUND) null else throw e
    }

    suspend fun landlordEmails(propertyId: String): Map<String, String> {
        val result = functions.getHttpsCallable("landlordEmails${CloudFunctionEnv.suffix}")
            .call(hashMapOf("propertyId" to propertyId))
            .await()
        val emails = (result.getData() as? Map<*, *>)?.get("emails") as? Map<*, *> ?: return emptyMap()
        return emails.entries.mapNotNull { (uid, email) ->
            if (uid is String && email is String) uid to email else null
        }.toMap()
    }

    /**
     * Pro is recorded only by the server: verifyPurchase{Env} checks the
     * purchase with Google Play and writes `proExpiryDate` on the profile
     * (clients can't write Pro fields — Firestore rules).
     */
    suspend fun verifyPlayPurchase(purchaseToken: String) {
        functions.getHttpsCallable("verifyPurchase${CloudFunctionEnv.suffix}")
            .call(hashMapOf("platform" to "android", "purchaseToken" to purchaseToken))
            .await()
    }

}

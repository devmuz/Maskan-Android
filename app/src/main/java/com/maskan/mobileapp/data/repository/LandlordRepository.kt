package com.maskan.mobileapp.data.repository

import com.maskan.mobileapp.data.model.Landlord
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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
class LandlordRepository(private val firestore: FirebaseFirestore) {
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
    suspend fun ensureProfileExists(landlordId: String, email: String?) {
        val doc = landlordsCollection.document(landlordId).get().await()
        if (doc.exists()) return
        val guessedCurrency = runCatching { Locale.getDefault().let { java.util.Currency.getInstance(it).currencyCode } }
            .getOrDefault("USD")
        landlordsCollection.document(landlordId).set(
            hashMapOf(
                "email" to email.orEmpty(),
                "currencyCode" to guessedCurrency,
            ),
        ).await()
    }

    suspend fun setCurrencyCode(landlordId: String, currencyCode: String) {
        landlordsCollection.document(landlordId).update("currencyCode", currencyCode).await()
    }

    /**
     * Co-owner lookup by email (feature-properties.md's `addCoOwner` flow).
     * Relies on the `landlords` collection's open read rule (any authenticated
     * user can read, not just self) — see feature-properties.md's "Co-owner
     * lookup fix". The prospective co-owner must already have a Maskan account.
     */
    /** One-off fetch — used to resolve a co-owner UID to a display email (feature-properties.md). */
    suspend fun getById(landlordId: String): Landlord? =
        landlordsCollection.document(landlordId).get().await().toObject(Landlord::class.java)

    suspend fun findByEmail(email: String): Landlord? =
        landlordsCollection.whereEqualTo("email", email.trim())
            .limit(1)
            .get()
            .await()
            .toObjects(Landlord::class.java)
            .firstOrNull()

    /**
     * Anchors the new Pro expiry to Firestore's **server** clock, not the
     * device clock (a locally-altered device date/time must not be able to
     * fake a longer subscription) — write a serverTimestamp field, re-read it
     * with a forced server fetch, then compute `periodDays` (+ a small grace
     * buffer) forward from that resolved server time. No Google Play
     * Developer API / Cloud Function round trip; this is purely a Firestore
     * server-timestamp trick.
     */
    suspend fun grantProEntitlement(landlordId: String, periodDays: Int, source: String) {
        val docRef = landlordsCollection.document(landlordId)
        docRef.update("proServerCheck", FieldValue.serverTimestamp()).await()
        val resolved = docRef.get(Source.SERVER).await()
        val serverNow = resolved.getDate("proServerCheck") ?: Date()
        val expiry = Date(serverNow.time + periodDays * DAY_MILLIS)
        docRef.update(mapOf("proExpiryDate" to expiry, "proSource" to source)).await()
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}

package com.maskan.mobileapp.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Play Console subscription product IDs — must be created exactly as these strings. */
object SubscriptionProducts {
    const val MONTHLY = "maskan_pro_monthly"
    const val YEARLY = "maskan_pro_yearly"
    val ALL = listOf(MONTHLY, YEARLY)

    /**
     * Nominal billing period plus a small grace window, so a subscriber who
     * doesn't reopen the app exactly on their renewal day isn't briefly
     * locked out before the next `restorePurchases()` re-extends it.
     */
    fun periodDays(productId: String): Int? = when (productId) {
        MONTHLY -> 30 + GRACE_DAYS
        YEARLY -> 365 + GRACE_DAYS
        else -> null
    }

    private const val GRACE_DAYS = 3
}

/**
 * Wraps Play Billing (client library only — no Google Play Developer API /
 * Cloud Function verification, by design; see `LandlordRepository.
 * grantProEntitlement` for how the expiry date is anchored to Firestore's
 * server clock instead). Mirrors iOS's `PurchaseService` — see
 * `02-data-models.md` -> "Free tier vs. Pro" and `08-landlord-settings.md`.
 */
class PurchaseRepository(context: Context, private val landlordRepository: LandlordRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Set by the landlord session (LandlordViewModel) right after sign-in; cleared on sign-out. */
    var activeLandlordId: String? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.let { list -> scope.launch { list.forEach { handlePurchase(it) } } }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _products = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val products: StateFlow<Map<String, ProductDetails>> = _products

    private var isConnected = false

    private suspend fun ensureConnected() {
        if (isConnected) return
        suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    isConnected = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    isConnected = false
                }
            })
        }
    }

    suspend fun loadProducts() {
        ensureConnected()
        if (!isConnected) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                SubscriptionProducts.ALL.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                },
            )
            .build()
        val result = billingClient.queryProductDetails(params)
        _products.value = result.productDetailsList.orEmpty().associateBy { it.productId }
    }

    /** The offer token comes from the product's (only) base plan — no special promotional offers configured. */
    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    /**
     * Called on landlord-session start: re-checks Play Billing (on-device,
     * cheap — not the Play Developer API) for any still-active subscription
     * and re-extends `proExpiryDate` if found. If a previously Android-
     * purchased subscription has lapsed, this simply stops finding it, and
     * the existing `proExpiryDate` in Firestore is left to expire naturally.
     */
    suspend fun restorePurchases() {
        ensureConnected()
        if (!isConnected) return
        val landlordId = activeLandlordId ?: return
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        val result = billingClient.queryPurchasesAsync(params)
        result.purchasesList
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { handlePurchase(it, landlordId) }
    }

    private suspend fun handlePurchase(purchase: Purchase, landlordId: String? = activeLandlordId) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())
        }
        val id = landlordId ?: return
        val productId = purchase.products.firstOrNull() ?: return
        val periodDays = SubscriptionProducts.periodDays(productId) ?: return
        landlordRepository.grantProEntitlement(id, periodDays, source = "android")
    }
}

package com.factory.driftlybabysleepsounds.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the connection to the Play Store billing service, exposes catalog/price info as flows, and
 * drives the purchase flow. [onVerifiedPurchases] is invoked with the current set of verified,
 * still-owned entitlements every time purchases are (re)queried, so callers should treat each
 * invocation as the full, authoritative set rather than a delta - this is what lets subscription
 * expiry be detected for free: Play simply stops returning the purchase once it lapses.
 */
class BillingManager(
    context: Context,
    private val onVerifiedPurchases: suspend (List<VerifiedPurchase>) -> Unit,
    billingClientFactory: (Context, PurchasesUpdatedListener) -> BillingClient = { ctx, listener ->
        BillingClient.newBuilder(ctx).setListener(listener).enablePendingPurchases().build()
    }
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val billingClient: BillingClient = billingClientFactory(appContext, this)

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _subscriptionProducts = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val subscriptionProducts: StateFlow<Map<String, ProductDetails>> = _subscriptionProducts.asStateFlow()

    private val _inAppProducts = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val inAppProducts: StateFlow<Map<String, ProductDetails>> = _inAppProducts.asStateFlow()

    private val _events = MutableSharedFlow<BillingUiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<BillingUiEvent> = _events.asSharedFlow()

    private var retryAttempt = 0

    fun startConnection() {
        if (billingClient.isReady || _connectionState.value == BillingConnectionState.CONNECTING) return
        _connectionState.value = BillingConnectionState.CONNECTING
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    retryAttempt = 0
                    _connectionState.value = BillingConnectionState.CONNECTED
                    scope.launch {
                        queryProductDetails()
                        queryOwnedPurchases()
                    }
                } else {
                    _connectionState.value = BillingConnectionState.UNAVAILABLE
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingConnectionState.DISCONNECTED
                retryWithBackoff()
            }
        })
    }

    private fun retryWithBackoff() {
        val delayMillis = minOf(30_000L, 1000L * (1L shl retryAttempt).coerceAtMost(30))
        retryAttempt++
        scope.launch {
            delay(delayMillis)
            startConnection()
        }
    }

    private suspend fun queryProductDetails() {
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                BillingProductIds.SUBSCRIPTION_IDS.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()
        val subsResult = billingClient.queryProductDetails(subsParams)
        if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _subscriptionProducts.value = subsResult.productDetailsList.orEmpty().associateBy { it.productId }
        }

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                BillingProductIds.IN_APP_IDS.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()
        val inAppResult = billingClient.queryProductDetails(inAppParams)
        if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _inAppProducts.value = inAppResult.productDetailsList.orEmpty().associateBy { it.productId }
        }
    }

    suspend fun queryOwnedPurchases() {
        if (!billingClient.isReady) return
        val subsPurchases = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )
        val inAppPurchases = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        val allPurchases = subsPurchases.purchasesList + inAppPurchases.purchasesList
        allPurchases.forEach { acknowledgeIfNeeded(it) }

        val verified = allPurchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .filter { PurchaseVerifier.isValid(it) }
            .flatMap { purchase ->
                purchase.products.map { productId ->
                    VerifiedPurchase(productId, purchase.purchaseToken, purchase.isAutoRenewing)
                }
            }
        onVerifiedPurchases(verified)
    }

    /** Re-queries Play for owned purchases; used for both "Restore purchases" and app-resume checks. */
    fun restorePurchases() {
        scope.launch {
            if (!billingClient.isReady) {
                _events.emit(BillingUiEvent.NetworkError)
                return@launch
            }
            queryOwnedPurchases()
            _events.emit(BillingUiEvent.PurchaseSuccess)
        }
    }

    fun launchSubscriptionPurchase(activity: Activity, productId: String) {
        val details = _subscriptionProducts.value[productId]
        val offerToken = details?.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (details == null || offerToken == null) {
            scope.launch { _events.emit(BillingUiEvent.NetworkError) }
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    fun launchInAppPurchase(activity: Activity, productId: String) {
        val details = _inAppProducts.value[productId]
        if (details == null) {
            scope.launch { _events.emit(BillingUiEvent.NetworkError) }
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val updatedPurchases = purchases.orEmpty()
                scope.launch {
                    updatedPurchases.forEach { purchase ->
                        when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> acknowledgeIfNeeded(purchase)
                            Purchase.PurchaseState.PENDING -> _events.emit(BillingUiEvent.PurchasePending)
                            else -> Unit
                        }
                    }
                    queryOwnedPurchases()
                    if (updatedPurchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                        _events.emit(BillingUiEvent.PurchaseSuccess)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                scope.launch { _events.emit(BillingUiEvent.PurchaseCancelled) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                scope.launch {
                    queryOwnedPurchases()
                    _events.emit(BillingUiEvent.AlreadyOwned)
                }
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.NETWORK_ERROR,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                scope.launch { _events.emit(BillingUiEvent.NetworkError) }
            }
            else -> {
                Log.w("BillingManager", "Purchase flow error: ${result.responseCode} ${result.debugMessage}")
                scope.launch { _events.emit(BillingUiEvent.Error(result.debugMessage)) }
            }
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isAcknowledged) return
        if (!PurchaseVerifier.isValid(purchase)) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params)
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}

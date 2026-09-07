package com.factory.driftlybabysleepsounds.ui.paywall

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.driftlybabysleepsounds.DriftlyApplication
import com.factory.driftlybabysleepsounds.billing.BillingConnectionState
import com.factory.driftlybabysleepsounds.billing.BillingProductIds
import com.factory.driftlybabysleepsounds.billing.BillingUiEvent
import com.factory.driftlybabysleepsounds.billing.FallbackPricing
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaywallPlanUi(
    val productId: String,
    val title: String,
    val priceLabel: String,
    val periodLabel: String,
    val badge: String? = null
)

data class PaywallUiState(
    val plans: List<PaywallPlanUi> = emptyList(),
    val removeAdsPriceLabel: String = FallbackPricing.FORMATTED_PRICE.getValue(BillingProductIds.REMOVE_ADS),
    val isStoreReady: Boolean = false,
    val isStoreUnavailable: Boolean = false
)

private val PLAN_META = mapOf(
    BillingProductIds.WEEKLY to Triple("Weekly", "/week", null),
    BillingProductIds.MONTHLY to Triple("Monthly", "/month", "Most popular"),
    BillingProductIds.YEARLY to Triple("Yearly", "/year", "Best value"),
    BillingProductIds.LIFETIME to Triple("Lifetime", "one-time", null)
)

class PaywallViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DriftlyApplication
    private val billingManager = app.billingManager

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val isPremium: StateFlow<Boolean> = app.premiumManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val uiState: StateFlow<PaywallUiState> = combine(
        billingManager.subscriptionProducts,
        billingManager.inAppProducts,
        billingManager.connectionState
    ) { subs, inApp, connection ->
        val plans = (BillingProductIds.SUBSCRIPTION_IDS + BillingProductIds.LIFETIME).map { productId ->
            val details = subs[productId] ?: inApp[productId]
            val (title, period, badge) = PLAN_META.getValue(productId)
            PaywallPlanUi(
                productId = productId,
                title = title,
                priceLabel = formattedPriceOf(details) ?: FallbackPricing.FORMATTED_PRICE.getValue(productId),
                periodLabel = period,
                badge = badge
            )
        }
        PaywallUiState(
            plans = plans,
            removeAdsPriceLabel = formattedPriceOf(inApp[BillingProductIds.REMOVE_ADS])
                ?: FallbackPricing.FORMATTED_PRICE.getValue(BillingProductIds.REMOVE_ADS),
            isStoreReady = connection == BillingConnectionState.CONNECTED,
            isStoreUnavailable = connection == BillingConnectionState.UNAVAILABLE
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaywallUiState())

    init {
        viewModelScope.launch {
            billingManager.events.collect { event -> handleEvent(event) }
        }
    }

    private suspend fun handleEvent(event: BillingUiEvent) {
        val message = when (event) {
            BillingUiEvent.PurchaseSuccess -> "You're all set! Premium is now unlocked."
            BillingUiEvent.PurchaseCancelled -> "Purchase cancelled."
            BillingUiEvent.PurchasePending -> "Purchase pending - premium unlocks automatically once it's approved."
            BillingUiEvent.AlreadyOwned -> "You already own this. Restoring your purchase."
            BillingUiEvent.NetworkError -> "No connection to the Play Store. Check your network and try again."
            is BillingUiEvent.Error -> "Something went wrong. Please try again."
        }
        _messages.emit(message)
    }

    private fun formattedPriceOf(details: ProductDetails?): String? {
        details ?: return null
        details.oneTimePurchaseOfferDetails?.let { return it.formattedPrice }
        return details.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
    }

    fun purchase(activity: Activity, productId: String) {
        if (!uiState.value.isStoreReady) {
            viewModelScope.launch { _messages.emit("No connection to the Play Store. Check your network and try again.") }
            return
        }
        if (productId == BillingProductIds.LIFETIME) {
            billingManager.launchInAppPurchase(activity, productId)
        } else {
            billingManager.launchSubscriptionPurchase(activity, productId)
        }
    }

    fun purchaseRemoveAds(activity: Activity) {
        if (!uiState.value.isStoreReady) {
            viewModelScope.launch { _messages.emit("No connection to the Play Store. Check your network and try again.") }
            return
        }
        billingManager.launchInAppPurchase(activity, BillingProductIds.REMOVE_ADS)
    }

    fun restore() {
        billingManager.restorePurchases()
    }
}

package com.factory.driftlybabysleepsounds.billing

enum class BillingConnectionState { DISCONNECTED, CONNECTING, CONNECTED, UNAVAILABLE }

/** A purchase that passed local signature verification and is ready to unlock an entitlement. */
data class VerifiedPurchase(
    val productId: String,
    val purchaseToken: String,
    val isAutoRenewing: Boolean
)

sealed interface BillingUiEvent {
    data object PurchaseSuccess : BillingUiEvent
    data object PurchaseCancelled : BillingUiEvent
    data object PurchasePending : BillingUiEvent
    data object AlreadyOwned : BillingUiEvent
    data object NetworkError : BillingUiEvent
    data class Error(val message: String) : BillingUiEvent
}

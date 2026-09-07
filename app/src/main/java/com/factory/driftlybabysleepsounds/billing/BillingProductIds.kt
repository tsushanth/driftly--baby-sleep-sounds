package com.factory.driftlybabysleepsounds.billing

/**
 * Play Console product IDs. Lifetime is a one-time (INAPP) product despite living under the
 * "subscription" id namespace, since Play Billing subscriptions must auto-renew.
 */
object BillingProductIds {
    const val WEEKLY = "com.factory.driftlybabysleepsounds.subscription.weekly"
    const val MONTHLY = "com.factory.driftlybabysleepsounds.subscription.monthly"
    const val YEARLY = "com.factory.driftlybabysleepsounds.subscription.yearly"
    const val LIFETIME = "com.factory.driftlybabysleepsounds.subscription.lifetime"
    const val REMOVE_ADS = "com.factory.driftlybabysleepsounds.remove_ads"

    val SUBSCRIPTION_IDS = listOf(WEEKLY, MONTHLY, YEARLY)
    val IN_APP_IDS = listOf(LIFETIME, REMOVE_ADS)
    val PREMIUM_UNLOCK_IDS = setOf(WEEKLY, MONTHLY, YEARLY, LIFETIME)
}

/** Store-listing prices used before [com.android.billingclient.api.ProductDetails] have loaded. */
object FallbackPricing {
    val FORMATTED_PRICE = mapOf(
        BillingProductIds.WEEKLY to "$2.55",
        BillingProductIds.MONTHLY to "$6.39",
        BillingProductIds.YEARLY to "$47.99",
        BillingProductIds.LIFETIME to "$95.98",
        BillingProductIds.REMOVE_ADS to "$1.99"
    )
}

package com.factory.driftlybabysleepsounds.premium

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.factory.driftlybabysleepsounds.billing.BillingProductIds
import com.factory.driftlybabysleepsounds.billing.VerifiedPurchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PremiumState(
    val isPremium: Boolean = false,
    val isAdsRemoved: Boolean = false,
    val activeProductId: String? = null,
    val hasSeenOnboardingPaywall: Boolean = false
)

private val Context.premiumDataStore by preferencesDataStore(name = "driftly_premium")

/**
 * Source of truth for the user's entitlement state. [applyVerifiedPurchases] is expected to be
 * called with the *complete* current set of verified purchases every time Play is queried, so a
 * subscription that Play silently stops returning (i.e. it expired or was cancelled and lapsed)
 * naturally clears premium here - there's no separate "expiry" code path to maintain.
 */
class PremiumManager(private val context: Context) {

    private object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val IS_ADS_REMOVED = booleanPreferencesKey("is_ads_removed")
        val ACTIVE_PRODUCT_ID = stringPreferencesKey("active_product_id")
        val HAS_SEEN_ONBOARDING_PAYWALL = booleanPreferencesKey("has_seen_onboarding_paywall")
    }

    val premiumState: Flow<PremiumState> = context.premiumDataStore.data.map { prefs ->
        PremiumState(
            isPremium = prefs[Keys.IS_PREMIUM] ?: false,
            isAdsRemoved = prefs[Keys.IS_ADS_REMOVED] ?: false,
            activeProductId = prefs[Keys.ACTIVE_PRODUCT_ID],
            hasSeenOnboardingPaywall = prefs[Keys.HAS_SEEN_ONBOARDING_PAYWALL] ?: false
        )
    }

    val isPremium: Flow<Boolean> = premiumState.map { it.isPremium }

    suspend fun applyVerifiedPurchases(purchases: List<VerifiedPurchase>) {
        val unlockingPurchase = purchases.firstOrNull { it.productId in BillingProductIds.PREMIUM_UNLOCK_IDS }
        val adsRemoved = unlockingPurchase != null || purchases.any { it.productId == BillingProductIds.REMOVE_ADS }
        context.premiumDataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = unlockingPurchase != null
            prefs[Keys.IS_ADS_REMOVED] = adsRemoved
            if (unlockingPurchase != null) {
                prefs[Keys.ACTIVE_PRODUCT_ID] = unlockingPurchase.productId
            } else {
                prefs.remove(Keys.ACTIVE_PRODUCT_ID)
            }
        }
    }

    suspend fun markOnboardingPaywallSeen() {
        context.premiumDataStore.edit { it[Keys.HAS_SEEN_ONBOARDING_PAYWALL] = true }
    }
}

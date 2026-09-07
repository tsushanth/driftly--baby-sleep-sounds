package com.factory.driftlybabysleepsounds.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.driftlybabysleepsounds.DriftlyApplication
import com.factory.driftlybabysleepsounds.billing.BillingManager
import com.factory.driftlybabysleepsounds.billing.BillingUiEvent
import com.factory.driftlybabysleepsounds.data.local.AppThemeMode
import com.factory.driftlybabysleepsounds.data.local.ThemePreferences
import com.factory.driftlybabysleepsounds.premium.PremiumManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DriftlyApplication
    private val themePreferences: ThemePreferences = app.themePreferences
    private val premiumManager: PremiumManager = app.premiumManager
    private val billingManager: BillingManager = app.billingManager

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val themeMode: StateFlow<AppThemeMode> = themePreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    val isPremium: StateFlow<Boolean> = premiumManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            billingManager.events.collect { event ->
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
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { themePreferences.setThemeMode(mode) }
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
        viewModelScope.launch { _messages.emit("Checking for previous purchases...") }
    }
}

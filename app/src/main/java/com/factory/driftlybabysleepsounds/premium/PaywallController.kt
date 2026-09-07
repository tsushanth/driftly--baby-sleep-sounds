package com.factory.driftlybabysleepsounds.premium

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PaywallTrigger {
    ONBOARDING, PREMIUM_SOUND, MULTI_MIX, SAVE_MIX, SLEEP_TIMER, SETTINGS_UPGRADE
}

/**
 * App-wide switch for showing the paywall, mirroring the [com.factory.driftlybabysleepsounds.audio.AudioEngine]
 * singleton pattern: any screen/view-model can request the paywall without needing a navigation
 * route or shared view model, and [com.factory.driftlybabysleepsounds.ui.DriftlyApp] just renders it.
 */
object PaywallController {
    private val _trigger = MutableStateFlow<PaywallTrigger?>(null)
    val trigger: StateFlow<PaywallTrigger?> = _trigger.asStateFlow()

    fun show(trigger: PaywallTrigger) {
        _trigger.value = trigger
    }

    fun dismiss() {
        _trigger.value = null
    }
}

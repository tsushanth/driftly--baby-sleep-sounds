package com.factory.driftlybabysleepsounds.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.factory.driftlybabysleepsounds.DriftlyApplication
import com.factory.driftlybabysleepsounds.audio.AudioEngine
import com.factory.driftlybabysleepsounds.premium.PaywallController
import com.factory.driftlybabysleepsounds.premium.PaywallTrigger
import com.factory.driftlybabysleepsounds.ui.home.HomeScreen
import com.factory.driftlybabysleepsounds.ui.navigation.DriftlyDestination
import com.factory.driftlybabysleepsounds.ui.paywall.PaywallScreen
import com.factory.driftlybabysleepsounds.ui.player.PlayerScreen
import com.factory.driftlybabysleepsounds.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.first

@Composable
fun DriftlyApp() {
    val navController = rememberNavController()
    val haptics = LocalHapticFeedback.current
    val activeVolumes by AudioEngine.activeVolumes.collectAsStateWithLifecycle()
    val paywallTrigger by PaywallController.trigger.collectAsStateWithLifecycle()
    val application = LocalContext.current.applicationContext as DriftlyApplication

    LaunchedEffect(Unit) {
        val state = application.premiumManager.premiumState.first()
        if (!state.isPremium && !state.hasSeenOnboardingPaywall) {
            application.premiumManager.markOnboardingPaywallSeen()
            PaywallController.show(PaywallTrigger.ONBOARDING)
        }
    }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                DriftlyDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            if (destination == DriftlyDestination.MIXER && activeVolumes.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text(activeVolumes.size.toString()) } }) {
                                    Icon(destination.icon, contentDescription = destination.label)
                                }
                            } else {
                                Icon(destination.icon, contentDescription = destination.label)
                            }
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DriftlyDestination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(DriftlyDestination.HOME.route) { HomeScreen() }
            composable(DriftlyDestination.MIXER.route) { PlayerScreen() }
            composable(DriftlyDestination.SETTINGS.route) { SettingsScreen() }
        }
    }

    if (paywallTrigger != null) {
        Dialog(
            onDismissRequest = { PaywallController.dismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PaywallScreen(onDismiss = { PaywallController.dismiss() })
        }
    }
}

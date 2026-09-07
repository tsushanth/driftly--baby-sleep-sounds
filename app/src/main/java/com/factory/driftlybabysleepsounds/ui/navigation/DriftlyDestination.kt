package com.factory.driftlybabysleepsounds.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class DriftlyDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Sounds", Icons.Filled.Home),
    MIXER("mixer", "Mixer", Icons.Filled.Tune),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}

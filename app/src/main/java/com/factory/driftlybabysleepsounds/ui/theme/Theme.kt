package com.factory.driftlybabysleepsounds.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = DuskLavender,
    onPrimary = NightBackgroundDark,
    secondary = StarGold,
    onSecondary = NightBackgroundDark,
    tertiary = SoftRose,
    background = NightBackgroundDark,
    onBackground = MoonlightCream,
    surface = NightSurfaceDark,
    onSurface = MoonlightCream,
    surfaceVariant = NightSurfaceVariantDark,
    onSurfaceVariant = CloudGray,
    error = ErrorRed
)

private val LightColors = lightColorScheme(
    primary = MidnightBlue,
    onPrimary = MoonlightCream,
    secondary = TwilightIndigo,
    onSecondary = MoonlightCream,
    tertiary = SoftRose,
    background = DayBackgroundLight,
    onBackground = MidnightBlue,
    surface = DaySurfaceLight,
    onSurface = MidnightBlue,
    surfaceVariant = DaySurfaceVariantLight,
    onSurfaceVariant = TwilightIndigo,
    error = ErrorRed
)

@Composable
fun DriftlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = DriftlyTypography,
        content = content
    )
}

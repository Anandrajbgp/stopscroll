package com.example.stopscroll.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = ElectricBlueSoft,
    tertiary = NeonPink,
    background = CharcoalBlack,
    surface = SurfaceBlack,
    surfaceVariant = SurfaceBlackSoft,
    onPrimary = CharcoalBlack,
    onSecondary = CharcoalBlack,
    onTertiary = CharcoalBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = WarningRed,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    secondary = ElectricBlueSoft,
    tertiary = NeonPink,
    background = CharcoalBlack,
    surface = SurfaceBlack,
    surfaceVariant = SurfaceBlackSoft,
    onPrimary = CharcoalBlack,
    onSecondary = CharcoalBlack,
    onTertiary = CharcoalBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = WarningRed,
    onError = TextPrimary
)

@Composable
fun StopScrollTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme || !dynamicColor) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

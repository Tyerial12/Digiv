package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DigiPulseHoloColorScheme = darkColorScheme(
    primary = HoloCyanPrimary,
    onPrimary = CyberVoidBg,
    primaryContainer = HoloCardElevated,
    onPrimaryContainer = HoloCyanPrimary,
    secondary = EvolutionGoldSecondary,
    onSecondary = CyberVoidBg,
    secondaryContainer = Color(0xFF3B2A05),
    onSecondaryContainer = EvolutionGoldSecondary,
    tertiary = PlasmaVioletStamina,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2B194D),
    onTertiaryContainer = Color(0xFFDDD6FE),
    background = CyberVoidBg,
    onBackground = CyberTextHigh,
    surface = HoloSlateSurface,
    onSurface = CyberTextHigh,
    surfaceVariant = HoloCardElevated,
    onSurfaceVariant = CyberTextMuted,
    outline = HoloBorderGlow,
    error = VirusCrimsonError,
    onError = Color.White
)

@Composable
fun DigiPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DigiPulseHoloColorScheme,
        typography = Typography,
        content = content
    )
}

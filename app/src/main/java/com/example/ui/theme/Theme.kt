package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DentalDarkTeal,
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF004D63),
    onPrimaryContainer = Color(0xFFBBE9FF),
    secondary = Color(0xFF94A3B8),
    background = DentalDarkBackground,
    surface = DentalDarkSurface,
    surfaceVariant = DentalDarkSurfaceVariant,
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = DentalTeal,
    onPrimary = Color.White,
    primaryContainer = DentalTealContainer,
    onPrimaryContainer = DentalOnTealContainer,
    secondary = DentalSlate,
    secondaryContainer = DentalSlateContainer,
    tertiary = DentalGold,
    tertiaryContainer = DentalGoldContainer,
    background = DentalBackground,
    surface = DentalSurface,
    surfaceVariant = DentalSurfaceVariant,
    outline = DentalOutline,
    outlineVariant = DentalOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent clinical dental aesthetic
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

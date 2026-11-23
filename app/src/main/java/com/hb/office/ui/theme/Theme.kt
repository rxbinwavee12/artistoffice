package com.hb.office.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CyanColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    secondary = Color(0xFF0284C7),
    primaryContainer = Color(0xFF0F172A),
    onPrimaryContainer = Color(0xFF38BDF8)
)

val VioletColorScheme = darkColorScheme(
    primary = Color(0xFFA855F7),
    secondary = Color(0xFF9333EA),
    primaryContainer = Color(0xFF1E1B4B),
    onPrimaryContainer = Color(0xFFA855F7)
)

val GoldColorScheme = darkColorScheme(
    primary = Color(0xFFF59E0B),
    secondary = Color(0xFFD97706),
    primaryContainer = Color(0xFF451A03),
    onPrimaryContainer = Color(0xFFF59E0B)
)

val RedColorScheme = darkColorScheme(
    primary = Color(0xFFEF4444),
    secondary = Color(0xFFDC2626),
    primaryContainer = Color(0xFF450A0A),
    onPrimaryContainer = Color(0xFFEF4444)
)

@Composable
fun HBBackofficeTheme(
    accentColor: String = "cyan",
    content: @Composable () -> Unit
) {
    val colorScheme = when (accentColor) {
        "violet" -> VioletColorScheme
        "gold" -> GoldColorScheme
        "red" -> RedColorScheme
        else -> CyanColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

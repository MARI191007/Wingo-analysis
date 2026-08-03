package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ImmersiveIndigoPrimary,
    onPrimary = TextPrimary,
    primaryContainer = ImmersiveSurface,
    onPrimaryContainer = TextPrimary,
    secondary = ImmersiveIndigoLight,
    onSecondary = ImmersiveBackground,
    tertiary = LiveGreen,
    background = ImmersiveBackground,
    onBackground = TextPrimary,
    surface = ImmersiveSurface,
    onSurface = TextPrimary,
    surfaceVariant = ImmersiveSurface,
    onSurfaceVariant = TextSecondary,
    outline = ImmersiveCardBorder
)

@Composable
fun WingoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}



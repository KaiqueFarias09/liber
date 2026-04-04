package com.example.liber.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LiberColorScheme = darkColorScheme(
    background = LiberBackground,
    surface = LiberSurface,
    surfaceVariant = LiberSurfaceVariant,
    onBackground = LiberTextPrimary,
    onSurface = LiberTextPrimary,
    onSurfaceVariant = LiberTextSecondary,
    primary = LiberAccent,
    onPrimary = LiberTextPrimary,
    secondaryContainer = LiberSurfaceVariant,
    onSecondaryContainer = LiberTextPrimary,
)

@Composable
fun LiberTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LiberColorScheme,
        typography = Typography,
        content = content
    )
}

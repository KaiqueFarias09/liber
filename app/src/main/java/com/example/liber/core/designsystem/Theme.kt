package com.example.liber.core.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.liber.data.repository.AccentColor
import com.example.liber.data.repository.ThemeMode

private val LiberLightColorScheme = lightColorScheme(
    primary = Rose500,
    onPrimary = Color.White,
    primaryContainer = Rose300,
    onPrimaryContainer = Rose900,

    secondary = Sepia500,
    onSecondary = Color.White,
    secondaryContainer = Sepia100,
    onSecondaryContainer = Sepia800,

    tertiary = Sage500,
    onTertiary = Color.White,
    tertiaryContainer = Sage100,
    onTertiaryContainer = Sage800,

    background = Neutral200,
    onBackground = Neutral950,

    surface = Neutral150,
    onSurface = Neutral950,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral600,

    outline = Neutral600,
    outlineVariant = Neutral400,

    error = Error600,
    onError = Color.White,
    errorContainer = Error100,
    onErrorContainer = Error800,

    inverseSurface = Neutral750,
    inverseOnSurface = Neutral100,
    inversePrimary = Rose400,

    scrim = Color.Black,
)

private val LiberDarkColorScheme = darkColorScheme(
    primary = Rose400,
    onPrimary = Rose900,
    primaryContainer = Rose800,
    onPrimaryContainer = Rose200,

    secondary = Sepia300,
    onSecondary = Sepia800,
    secondaryContainer = Sepia850,
    onSecondaryContainer = Sepia100,

    tertiary = Sage300,
    onTertiary = Sage800,
    tertiaryContainer = Sage850,
    onTertiaryContainer = Sage100,

    background = Neutral900,
    onBackground = Neutral150,

    surface = Neutral850,
    onSurface = Neutral150,
    surfaceVariant = Neutral700,
    onSurfaceVariant = Neutral300,

    outline = Neutral300,
    outlineVariant = Neutral750,

    error = Error200,
    onError = Error700,
    errorContainer = Error700,
    onErrorContainer = Error100,

    inverseSurface = Neutral150,
    inverseOnSurface = Neutral800,
    inversePrimary = Rose500,

    scrim = Color.Black,
)

private fun getLiberLightColorScheme(accentColor: AccentColor): ColorScheme {
    val (primary, primaryContainer, onPrimaryContainer) = when (accentColor) {
        AccentColor.ROSE -> Triple(Rose500, Rose300, Rose900)
        AccentColor.SEPIA -> Triple(Sepia500, Sepia300, Sepia800)
        AccentColor.SAGE -> Triple(Sage500, Sage300, Sage800)
        AccentColor.BLUE -> Triple(Blue500, Blue300, Blue900)
        AccentColor.PURPLE -> Triple(Purple500, Purple300, Purple900)
        AccentColor.YELLOW -> Triple(Yellow500, Yellow300, Yellow900)
    }

    return LiberLightColorScheme.copy(
        primary = primary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        surfaceTint = primary
    )
}

private fun getLiberDarkColorScheme(accentColor: AccentColor): ColorScheme {
    val (primary, onPrimary, primaryContainer, onPrimaryContainer) = when (accentColor) {
        AccentColor.ROSE -> listOf(Rose400, Rose900, Rose800, Rose200)
        AccentColor.SEPIA -> listOf(Sepia300, Sepia800, Sepia850, Sepia100)
        AccentColor.SAGE -> listOf(Sage300, Sage800, Sage850, Sage100)
        AccentColor.BLUE -> listOf(Blue400, Blue900, Blue800, Blue200)
        AccentColor.PURPLE -> listOf(Purple400, Purple900, Purple800, Purple200)
        AccentColor.YELLOW -> listOf(Yellow400, Yellow900, Yellow800, Yellow200)
    }

    return LiberDarkColorScheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        surfaceTint = primary
    )
}

@Composable
fun LiberTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    accentColor: AccentColor = AccentColor.ROSE,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) {
        getLiberDarkColorScheme(accentColor)
    } else {
        getLiberLightColorScheme(accentColor)
    }
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                !darkTheme
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

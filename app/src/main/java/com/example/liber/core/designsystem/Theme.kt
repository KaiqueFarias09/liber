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

private data class PrimaryRoles(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
)

private val LiberLightColorScheme = lightColorScheme(
    primary = Rose500,
    onPrimary = Color.White,
    primaryContainer = Rose100,
    onPrimaryContainer = Rose900,

    secondary = Sepia500,
    onSecondary = Color.White,
    secondaryContainer = Sepia100,
    onSecondaryContainer = Sepia800,

    tertiary = Sage500,
    onTertiary = Color.White,
    tertiaryContainer = Sage100,
    onTertiaryContainer = Sage800,

    background = Neutral100,
    onBackground = Neutral950,

    surface = Neutral100,
    onSurface = Neutral950,
    surfaceTint = Rose500,
    surfaceBright = Neutral50,
    surfaceDim = Neutral150,
    surfaceContainerLowest = Neutral50,
    surfaceContainerLow = Neutral100,
    surfaceContainer = Neutral150,
    surfaceContainerHigh = Neutral200,
    surfaceContainerHighest = Neutral300,
    surfaceVariant = Neutral200,
    onSurfaceVariant = Neutral600,

    outline = Neutral400,
    outlineVariant = Neutral300,

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

    surface = Neutral900,
    onSurface = Neutral150,
    surfaceTint = Rose400,
    surfaceBright = Neutral800,
    surfaceDim = Neutral900,
    surfaceContainerLowest = Neutral950,
    surfaceContainerLow = Neutral900,
    surfaceContainer = Neutral850,
    surfaceContainerHigh = Neutral800,
    surfaceContainerHighest = Neutral750,
    surfaceVariant = Neutral700,
    onSurfaceVariant = Neutral300,

    outline = Neutral300,
    outlineVariant = Neutral700,

    error = Error200,
    onError = Error700,
    errorContainer = Error700,
    onErrorContainer = Error100,

    inverseSurface = Neutral150,
    inverseOnSurface = Neutral800,
    inversePrimary = Rose500,

    scrim = Color.Black,
)

private fun lightPrimaryRoles(accentColor: AccentColor): PrimaryRoles = when (accentColor) {
    AccentColor.ROSE -> PrimaryRoles(
        primary = Rose500,
        onPrimary = Color.White,
        primaryContainer = Rose100,
        onPrimaryContainer = Rose900,
        inversePrimary = Rose400,
    )
    AccentColor.SEPIA -> PrimaryRoles(
        primary = Sepia500,
        onPrimary = Color.White,
        primaryContainer = Sepia100,
        onPrimaryContainer = Sepia900,
        inversePrimary = Sepia300,
    )
    AccentColor.SAGE -> PrimaryRoles(
        primary = Sage500,
        onPrimary = Color.White,
        primaryContainer = Sage100,
        onPrimaryContainer = Sage900,
        inversePrimary = Sage300,
    )
    AccentColor.BLUE -> PrimaryRoles(
        primary = Blue500,
        onPrimary = Color.White,
        primaryContainer = Blue100,
        onPrimaryContainer = Blue900,
        inversePrimary = Blue300,
    )
    AccentColor.PURPLE -> PrimaryRoles(
        primary = Purple500,
        onPrimary = Color.White,
        primaryContainer = Purple100,
        onPrimaryContainer = Purple900,
        inversePrimary = Purple300,
    )
    AccentColor.YELLOW -> PrimaryRoles(
        primary = Yellow500,
        onPrimary = Yellow900,
        primaryContainer = Yellow100,
        onPrimaryContainer = Yellow900,
        inversePrimary = Yellow300,
    )
}

private fun darkPrimaryRoles(accentColor: AccentColor): PrimaryRoles = when (accentColor) {
    AccentColor.ROSE -> PrimaryRoles(
        primary = Rose400,
        onPrimary = Rose900,
        primaryContainer = Rose800,
        onPrimaryContainer = Rose200,
        inversePrimary = Rose500,
    )
    AccentColor.SEPIA -> PrimaryRoles(
        primary = Sepia300,
        onPrimary = Sepia900,
        primaryContainer = Sepia850,
        onPrimaryContainer = Sepia100,
        inversePrimary = Sepia500,
    )
    AccentColor.SAGE -> PrimaryRoles(
        primary = Sage300,
        onPrimary = Sage900,
        primaryContainer = Sage850,
        onPrimaryContainer = Sage100,
        inversePrimary = Sage500,
    )
    AccentColor.BLUE -> PrimaryRoles(
        primary = Blue300,
        onPrimary = Blue900,
        primaryContainer = Blue800,
        onPrimaryContainer = Blue100,
        inversePrimary = Blue500,
    )
    AccentColor.PURPLE -> PrimaryRoles(
        primary = Purple300,
        onPrimary = Purple900,
        primaryContainer = Purple800,
        onPrimaryContainer = Purple100,
        inversePrimary = Purple500,
    )
    AccentColor.YELLOW -> PrimaryRoles(
        primary = Yellow300,
        onPrimary = Yellow900,
        primaryContainer = Yellow800,
        onPrimaryContainer = Yellow100,
        inversePrimary = Yellow500,
    )
}

private fun getLiberLightColorScheme(accentColor: AccentColor): ColorScheme {
    val primaryRoles = lightPrimaryRoles(accentColor)

    return LiberLightColorScheme.copy(
        primary = primaryRoles.primary,
        onPrimary = primaryRoles.onPrimary,
        primaryContainer = primaryRoles.primaryContainer,
        onPrimaryContainer = primaryRoles.onPrimaryContainer,
        inversePrimary = primaryRoles.inversePrimary,
        surfaceTint = primaryRoles.primary,
    )
}

private fun getLiberDarkColorScheme(accentColor: AccentColor): ColorScheme {
    val primaryRoles = darkPrimaryRoles(accentColor)

    return LiberDarkColorScheme.copy(
        primary = primaryRoles.primary,
        onPrimary = primaryRoles.onPrimary,
        primaryContainer = primaryRoles.primaryContainer,
        onPrimaryContainer = primaryRoles.onPrimaryContainer,
        inversePrimary = primaryRoles.inversePrimary,
        surfaceTint = primaryRoles.primary,
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

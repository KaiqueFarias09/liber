package com.example.liber.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.liber.data.prefs.ThemeMode

private val LiberLightColorScheme = lightColorScheme(
    primary                = Rose500,
    onPrimary              = Color.White,
    primaryContainer       = Rose300,
    onPrimaryContainer     = Rose900,

    secondary              = Mauve500,
    onSecondary            = Color.White,
    secondaryContainer     = Mauve300,
    onSecondaryContainer   = Mauve800,

    tertiary               = Sage500,
    onTertiary             = Color.White,
    tertiaryContainer      = Sage200,
    onTertiaryContainer    = Sage800,

    background             = Neutral50,
    onBackground           = Neutral950,

    surface                = Neutral100,
    onSurface              = Neutral950,
    surfaceVariant         = Neutral150,
    onSurfaceVariant       = Neutral600,
    surfaceTint            = Color.Transparent,

    outline                = Neutral400,
    outlineVariant         = Neutral200,

    error                  = Error600,
    onError                = Color.White,
    errorContainer         = Error100,
    onErrorContainer       = Error800,

    inverseSurface         = Neutral750,
    inverseOnSurface       = Neutral100,
    inversePrimary         = Rose400,

    scrim                  = Color.Black,
)

private val LiberDarkColorScheme = darkColorScheme(
    primary                = Rose400,
    onPrimary              = Rose900,
    primaryContainer       = Rose800,
    onPrimaryContainer     = Rose200,

    secondary              = Mauve300,
    onSecondary            = Mauve800,
    secondaryContainer     = Mauve850,
    onSecondaryContainer   = Mauve100,

    tertiary               = Sage300,
    onTertiary             = Sage800,
    tertiaryContainer      = Sage850,
    onTertiaryContainer    = Sage100,

    background             = Neutral900,
    onBackground           = Neutral150,

    surface                = Neutral850,
    onSurface              = Neutral150,
    surfaceVariant         = Neutral700,
    onSurfaceVariant       = Neutral300,
    surfaceTint            = Color.Transparent,

    outline                = Neutral400,
    outlineVariant         = Neutral700,

    error                  = Error200,
    onError                = Error700,
    errorContainer         = Error700,
    onErrorContainer       = Error100,

    inverseSurface         = Neutral150,
    inverseOnSurface       = Neutral800,
    inversePrimary         = Rose500,

    scrim                  = Color.Black,
)

@Composable
fun LiberTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) LiberDarkColorScheme else LiberLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

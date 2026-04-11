package com.example.liber.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColorScheme(
    val roseContainer: Color,
    val onRoseContainer: Color,
    val yellowContainer: Color,
    val onYellowContainer: Color,
    val orangeContainer: Color,
    val onOrangeContainer: Color,
    val blueContainer: Color,
    val onBlueContainer: Color,
    val redContainer: Color,
    val onRedContainer: Color,
    val greenContainer: Color,
    val onGreenContainer: Color,
    val purpleContainer: Color,
    val onPurpleContainer: Color,
    val tealContainer: Color,
    val onTealContainer: Color,
    val isDark: Boolean,
)

val LightExtendedColors = ExtendedColorScheme(
    roseContainer = PastelRose100, onRoseContainer = PastelRose700,
    yellowContainer = PastelYellow100, onYellowContainer = PastelYellow700,
    orangeContainer = PastelOrange100, onOrangeContainer = PastelOrange700,
    blueContainer = PastelBlue100, onBlueContainer = PastelBlue700,
    redContainer = PastelRed100, onRedContainer = PastelRed700,
    greenContainer = PastelGreen100, onGreenContainer = PastelGreen700,
    purpleContainer = PastelPurple100, onPurpleContainer = PastelPurple700,
    tealContainer = PastelTeal100, onTealContainer = PastelTeal700,
    isDark = false,
)

val DarkExtendedColors = ExtendedColorScheme(
    roseContainer = PastelRose850, onRoseContainer = PastelRose300,
    yellowContainer = PastelYellow850, onYellowContainer = PastelYellow300,
    orangeContainer = PastelOrange850, onOrangeContainer = PastelOrange300,
    blueContainer = PastelBlue850, onBlueContainer = PastelBlue300,
    redContainer = PastelRed850, onRedContainer = PastelRed300,
    greenContainer = PastelGreen850, onGreenContainer = PastelGreen300,
    purpleContainer = PastelPurple850, onPurpleContainer = PastelPurple300,
    tealContainer = PastelTeal850, onTealContainer = PastelTeal300,
    isDark = true,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

// Access extended colors via MaterialTheme.extendedColors.yellowContainer
val MaterialTheme.extendedColors: ExtendedColorScheme
    @Composable get() = LocalExtendedColors.current

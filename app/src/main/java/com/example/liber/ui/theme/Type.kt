package com.example.liber.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.liber.R

// Gambetta — serif, used for titles and headlines
val Gambetta = FontFamily(
    Font(R.font.gambetta_light,          FontWeight.Light),
    Font(R.font.gambetta_light_italic,   FontWeight.Light,    FontStyle.Italic),
    Font(R.font.gambetta_regular,        FontWeight.Normal),
    Font(R.font.gambetta_italic,         FontWeight.Normal,   FontStyle.Italic),
    Font(R.font.gambetta_medium,         FontWeight.Medium),
    Font(R.font.gambetta_medium_italic,  FontWeight.Medium,   FontStyle.Italic),
    Font(R.font.gambetta_semibold,       FontWeight.SemiBold),
    Font(R.font.gambetta_semibold_italic,FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.gambetta_bold,           FontWeight.Bold),
    Font(R.font.gambetta_bold_italic,    FontWeight.Bold,     FontStyle.Italic),
)

// Switzer — sans-serif, used for body and labels
val Switzer = FontFamily(
    Font(R.font.switzer_light,           FontWeight.Light),
    Font(R.font.switzer_light_italic,    FontWeight.Light,    FontStyle.Italic),
    Font(R.font.switzer_regular,         FontWeight.Normal),
    Font(R.font.switzer_italic,          FontWeight.Normal,   FontStyle.Italic),
    Font(R.font.switzer_medium,          FontWeight.Medium),
    Font(R.font.switzer_medium_italic,   FontWeight.Medium,   FontStyle.Italic),
    Font(R.font.switzer_semibold,        FontWeight.SemiBold),
    Font(R.font.switzer_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.switzer_bold,            FontWeight.Bold),
    Font(R.font.switzer_bold_italic,     FontWeight.Bold,     FontStyle.Italic),
)

val Typography = Typography(
    // ── Display ──────────────────────────────────────────────────────────────
    // Hero/splash text — Gambetta Bold
    displayLarge = TextStyle(
        fontFamily = Gambetta,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Gambetta,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Gambetta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),

    // ── Headline ─────────────────────────────────────────────────────────────
    // Screen-level titles — Gambetta SemiBold/Medium
    headlineLarge = TextStyle(
        fontFamily = Gambetta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Gambetta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Gambetta,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),

    // ── Title ─────────────────────────────────────────────────────────────────
    // Section headers and card titles
    // titleLarge → Gambetta (section headings that benefit from the serif look)
    titleLarge = TextStyle(
        fontFamily = Gambetta,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    // titleMedium / titleSmall → Switzer (tighter UI titles like card names)
    titleMedium = TextStyle(
        fontFamily = Switzer,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Switzer,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // ── Body ─────────────────────────────────────────────────────────────────
    // Main reading/content text — Switzer Regular
    bodyLarge = TextStyle(
        fontFamily = Switzer,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Switzer,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Switzer,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // ── Label ─────────────────────────────────────────────────────────────────
    // Bottom nav, chips, captions — Switzer Medium/Regular
    labelLarge = TextStyle(
        fontFamily = Switzer,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Switzer,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Switzer,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

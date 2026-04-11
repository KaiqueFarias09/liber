package com.example.liber.feature.reader

import androidx.compose.ui.graphics.Color
import com.example.liber.R
import com.example.liber.core.util.UiText

data class ReaderTheme(
    val id: String,
    val name: UiText,
    val background: Color,
    val textColor: Color,
    val isDark: Boolean,
    // CSS-compatible hex color for the ::selection background in the WebView
    // (must be a 6 or 8 digit hex string like "#AARRGGBB" or "rgba(r,g,b,a)")
    val selectionColorCss: String,
)

val ReaderThemes = listOf(
    // Dark themes → blue-family selection
    ReaderTheme(
        "original",
        UiText.StringResource(R.string.reader_theme_original),
        Color(0xFF111111),
        Color(0xFFE5E5EA),
        true,
        "rgba(10,  132, 255, 0.55)"
    ), // iOS blue
    ReaderTheme(
        "quiet",
        UiText.StringResource(R.string.reader_theme_quiet),
        Color(0xFF2C2C2E),
        Color(0xFFD1D1D6),
        true,
        "rgba(64,  156, 255, 0.50)"
    ), // softer blue
    ReaderTheme(
        "bold",
        UiText.StringResource(R.string.reader_theme_bold),
        Color(0xFF000000),
        Color(0xFFFFFFFF),
        true,
        "rgba(10,  132, 255, 0.60)"
    ), // vivid iOS blue on pure black
    ReaderTheme(
        "focus",
        UiText.StringResource(R.string.reader_theme_focus),
        Color(0xFF2D2822),
        Color(0xFFE0D6C8),
        true,
        "rgba(90,  170, 255, 0.45)"
    ), // muted blue on warm dark

    // Light/beige themes → warm selection
    ReaderTheme(
        "paper",
        UiText.StringResource(R.string.reader_theme_paper),
        Color(0xFFF4F4F0),
        Color(0xFF111111),
        false,
        "rgba(10,  132, 255, 0.35)"
    ), // standard blue on white
    ReaderTheme(
        "calm",
        UiText.StringResource(R.string.reader_theme_calm),
        Color(0xFFF5EBD9),
        Color(0xFF4A3F32),
        false,
        "rgba(210, 120,  50, 0.40)"
    ), // warm amber on parchment
)

fun findReaderTheme(id: String): ReaderTheme =
    ReaderThemes.find { it.id == id } ?: ReaderThemes.first()

package com.example.liber.ui.reader

import androidx.compose.ui.graphics.Color

data class ReaderTheme(
    val id: String,
    val name: String,
    val background: Color,
    val textColor: Color,
    val isDark: Boolean,
)

val ReaderThemes = listOf(
    ReaderTheme("original", "Original", Color(0xFF111111), Color(0xFFE5E5EA), true),
    ReaderTheme("quiet",    "Quiet",    Color(0xFF2C2C2E), Color(0xFFD1D1D6), true),
    ReaderTheme("paper",    "Paper",    Color(0xFFF4F4F0), Color(0xFF111111), false),
    ReaderTheme("bold",     "Bold",     Color(0xFF000000), Color(0xFFFFFFFF), true),
    ReaderTheme("calm",     "Calm",     Color(0xFFF5EBD9), Color(0xFF4A3F32), false),
    ReaderTheme("focus",    "Focus",    Color(0xFF2D2822), Color(0xFFE0D6C8), true),
)

fun findReaderTheme(id: String): ReaderTheme =
    ReaderThemes.find { it.id == id } ?: ReaderThemes.first()

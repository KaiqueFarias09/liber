package com.example.liber.feature.library

import com.example.liber.R
import com.example.liber.core.util.UiText

enum class LibrarySortOption(val label: UiText) {
    RECENT(UiText.StringResource(R.string.sort_option_recent)),
    TITLE(UiText.StringResource(R.string.sort_option_title)),
    AUTHOR(UiText.StringResource(R.string.sort_option_author)),
}

enum class LibraryViewMode { GRID, LIST }

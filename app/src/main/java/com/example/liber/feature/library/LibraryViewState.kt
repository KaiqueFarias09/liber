package com.example.liber.feature.library

import com.example.liber.R
import com.example.liber.core.util.UiText

enum class LibrarySortOption(val label: UiText) {
    RECENT(UiText.StringResource(R.string.sort_option_recent)),
    TITLE(UiText.StringResource(R.string.sort_option_title)),
    AUTHOR(UiText.StringResource(R.string.sort_option_author)),
    PROGRESS(UiText.StringResource(R.string.sort_option_progress)),
}

enum class LibraryFilterStatus(val label: UiText) {
    ALL(UiText.StringResource(R.string.filter_all)),
    UNREAD(UiText.StringResource(R.string.filter_unread)),
    IN_PROGRESS(UiText.StringResource(R.string.filter_in_progress)),
    FINISHED(UiText.StringResource(R.string.filter_finished))
}

enum class LibraryViewMode { GRID, LIST }

enum class SearchType {
    ALL, BOOKS, AUDIOBOOKS, DICTIONARY
}

package com.example.liber.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseRepository
import com.example.liber.feature.library.CollectionSortOption
import com.example.liber.feature.library.LibrarySortOption
import com.example.liber.feature.library.LibraryViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    AUTO, DARK, LIGHT
}

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    appLogger: AppLogger,
) : BaseRepository("UserPreferencesRepository", appLogger) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val READER_THEME = stringPreferencesKey("reader_theme")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val PAGE_SCROLL = booleanPreferencesKey("page_scroll")
        val CUSTOMIZE_LAYOUT = booleanPreferencesKey("customize_layout")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val CHAR_SPACING = floatPreferencesKey("char_spacing")
        val WORD_SPACING = floatPreferencesKey("word_spacing")
        val MARGINS = floatPreferencesKey("margins")
        val COLUMN_COUNT = stringPreferencesKey("column_count")
        val JUSTIFY_TEXT = booleanPreferencesKey("justify_text")
        val BOOKS_VIEW_MODE = stringPreferencesKey("books_view_mode")
        val BOOKS_SORT_OPTION = stringPreferencesKey("books_sort_option")
        val AUDIOBOOKS_VIEW_MODE = stringPreferencesKey("audiobooks_view_mode")
        val AUDIOBOOKS_SORT_OPTION = stringPreferencesKey("audiobooks_sort_option")
        val READING_GOAL_MINUTES = intPreferencesKey("reading_goal_minutes")
        val DICTIONARY_HISTORY_ENABLED = booleanPreferencesKey("dictionary_history_enabled")
        val DICTIONARY_HISTORY_RETENTION_DAYS =
            intPreferencesKey("dictionary_history_retention_days")
        val SMART_RECOGNITION_INFO_DISMISSED =
            booleanPreferencesKey("smart_recognition_info_dismissed")
        val AUTO_COLLECTIONS_ENABLED = booleanPreferencesKey("auto_collections_enabled")
        val COLLECTIONS_SORT_OPTION = stringPreferencesKey("collections_sort_option")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.AUTO.name
            ThemeMode.valueOf(themeName)
        }

    suspend fun setThemeMode(themeMode: ThemeMode) = executeOperation(
        operationName = "setThemeMode",
        parameters = mapOf("themeMode" to themeMode.name),
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    val readerTheme: Flow<String> = dataStore.data
        .map { it[PreferencesKeys.READER_THEME] ?: "original" }

    suspend fun setReaderTheme(id: String) = executeOperation(
        operationName = "setReaderTheme",
        parameters = mapOf("id" to id),
    ) {
        dataStore.edit { it[PreferencesKeys.READER_THEME] = id }
    }

    val fontSize: Flow<Float> = dataStore.data
        .map { it[PreferencesKeys.FONT_SIZE] ?: 1.0f }

    suspend fun setFontSize(size: Float) = executeOperation(
        operationName = "setFontSize",
        parameters = mapOf("size" to size),
    ) {
        dataStore.edit { it[PreferencesKeys.FONT_SIZE] = size }
    }

    val pageScroll: Flow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.PAGE_SCROLL] ?: false }

    suspend fun setPageScroll(enabled: Boolean) = executeOperation(
        operationName = "setPageScroll",
        parameters = mapOf("enabled" to enabled),
    ) {
        dataStore.edit { it[PreferencesKeys.PAGE_SCROLL] = enabled }
    }

    val customizeLayout: Flow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.CUSTOMIZE_LAYOUT] ?: false }

    suspend fun setCustomizeLayout(enabled: Boolean) = executeOperation(
        operationName = "setCustomizeLayout",
        parameters = mapOf("enabled" to enabled),
    ) {
        dataStore.edit { it[PreferencesKeys.CUSTOMIZE_LAYOUT] = enabled }
    }

    val lineSpacing: Flow<Float> = dataStore.data
        .map { it[PreferencesKeys.LINE_SPACING] ?: 1.4f }

    suspend fun setLineSpacing(value: Float) = executeOperation(
        operationName = "setLineSpacing",
        parameters = mapOf("value" to value),
    ) {
        dataStore.edit { it[PreferencesKeys.LINE_SPACING] = value }
    }

    val charSpacing: Flow<Float> = dataStore.data
        .map { it[PreferencesKeys.CHAR_SPACING] ?: 0.0f }

    suspend fun setCharSpacing(value: Float) = executeOperation(
        operationName = "setCharSpacing",
        parameters = mapOf("value" to value),
    ) {
        dataStore.edit { it[PreferencesKeys.CHAR_SPACING] = value }
    }

    val wordSpacing: Flow<Float> = dataStore.data
        .map { it[PreferencesKeys.WORD_SPACING] ?: 0.0f }

    suspend fun setWordSpacing(value: Float) = executeOperation(
        operationName = "setWordSpacing",
        parameters = mapOf("value" to value),
    ) {
        dataStore.edit { it[PreferencesKeys.WORD_SPACING] = value }
    }

    val margins: Flow<Float> = dataStore.data
        .map { it[PreferencesKeys.MARGINS] ?: 24.0f }

    suspend fun setMargins(value: Float) = executeOperation(
        operationName = "setMargins",
        parameters = mapOf("value" to value),
    ) {
        dataStore.edit { it[PreferencesKeys.MARGINS] = value }
    }

    val columnCount: Flow<String> = dataStore.data
        .map { it[PreferencesKeys.COLUMN_COUNT] ?: "auto" }

    suspend fun setColumnCount(value: String) = executeOperation(
        operationName = "setColumnCount",
        parameters = mapOf("value" to value),
    ) {
        dataStore.edit { it[PreferencesKeys.COLUMN_COUNT] = value }
    }

    val justifyText: Flow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.JUSTIFY_TEXT] ?: false }

    suspend fun setJustifyText(value: Boolean) = executeOperation(
        operationName = "setJustifyText",
        parameters = mapOf("value" to value),
    ) {
        dataStore.edit { it[PreferencesKeys.JUSTIFY_TEXT] = value }
    }

    val booksViewMode: Flow<LibraryViewMode> = dataStore.data
        .map { preferences ->
            val name = preferences[PreferencesKeys.BOOKS_VIEW_MODE] ?: LibraryViewMode.GRID.name
            LibraryViewMode.valueOf(name)
        }

    suspend fun setBooksViewMode(mode: LibraryViewMode) = executeOperation(
        operationName = "setBooksViewMode",
        parameters = mapOf("mode" to mode.name),
    ) {
        dataStore.edit { it[PreferencesKeys.BOOKS_VIEW_MODE] = mode.name }
    }

    val booksSortOption: Flow<LibrarySortOption> = dataStore.data
        .map { preferences ->
            val name =
                preferences[PreferencesKeys.BOOKS_SORT_OPTION] ?: LibrarySortOption.RECENT.name
            LibrarySortOption.valueOf(name)
        }

    suspend fun setBooksSortOption(option: LibrarySortOption) = executeOperation(
        operationName = "setBooksSortOption",
        parameters = mapOf("option" to option.name),
    ) {
        dataStore.edit { it[PreferencesKeys.BOOKS_SORT_OPTION] = option.name }
    }

    val audiobooksViewMode: Flow<LibraryViewMode> = dataStore.data
        .map { preferences ->
            val name =
                preferences[PreferencesKeys.AUDIOBOOKS_VIEW_MODE] ?: LibraryViewMode.GRID.name
            LibraryViewMode.valueOf(name)
        }

    suspend fun setAudiobooksViewMode(mode: LibraryViewMode) = executeOperation(
        operationName = "setAudiobooksViewMode",
        parameters = mapOf("mode" to mode.name),
    ) {
        dataStore.edit { it[PreferencesKeys.AUDIOBOOKS_VIEW_MODE] = mode.name }
    }

    val audiobooksSortOption: Flow<LibrarySortOption> = dataStore.data
        .map { preferences ->
            val name =
                preferences[PreferencesKeys.AUDIOBOOKS_SORT_OPTION] ?: LibrarySortOption.RECENT.name
            LibrarySortOption.valueOf(name)
        }

    val dictionaryHistoryEnabled: Flow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.DICTIONARY_HISTORY_ENABLED] ?: true }

    val dictionaryHistoryRetentionDays: Flow<Int> = dataStore.data
        .map { it[PreferencesKeys.DICTIONARY_HISTORY_RETENTION_DAYS] ?: 90 }

    val readingGoalMinutes: Flow<Int> = dataStore.data
        .map { it[PreferencesKeys.READING_GOAL_MINUTES] ?: 30 }

    val smartRecognitionInfoDismissed: Flow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.SMART_RECOGNITION_INFO_DISMISSED] ?: false }

    val autoCollectionsEnabled: Flow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.AUTO_COLLECTIONS_ENABLED] ?: true }

    val collectionsSortOption: Flow<CollectionSortOption> = dataStore.data
        .map { preferences ->
            val name = preferences[PreferencesKeys.COLLECTIONS_SORT_OPTION]
                ?: CollectionSortOption.RECENT.name
            CollectionSortOption.valueOf(name)
        }

    suspend fun setCollectionsSortOption(option: CollectionSortOption) = executeOperation(
        operationName = "setCollectionsSortOption",
        parameters = mapOf("option" to option.name),
    ) {
        dataStore.edit { it[PreferencesKeys.COLLECTIONS_SORT_OPTION] = option.name }
    }

    val recentSearches: Flow<List<String>> = dataStore.data
        .map { preferences ->
            val searches = preferences[PreferencesKeys.RECENT_SEARCHES] ?: ""
            if (searches.isBlank()) emptyList() else searches.split("|")
        }

    suspend fun addRecentSearch(query: String) = executeOperation(
        operationName = "addRecentSearch",
        parameters = mapOf("query" to query),
    ) {
        if (query.isBlank()) return@executeOperation
        dataStore.edit { preferences ->
            val currentSearches = preferences[PreferencesKeys.RECENT_SEARCHES]
                ?.split("|")
                ?.toMutableList() ?: mutableListOf()

            currentSearches.remove(query)
            currentSearches.add(0, query)

            val limitedSearches = currentSearches.take(5)
            preferences[PreferencesKeys.RECENT_SEARCHES] = limitedSearches.joinToString("|")
        }
    }

    suspend fun setAudiobooksSortOption(option: LibrarySortOption) = executeOperation(
        operationName = "setAudiobooksSortOption",
        parameters = mapOf("option" to option.name),
    ) {
        dataStore.edit { it[PreferencesKeys.AUDIOBOOKS_SORT_OPTION] = option.name }
    }

    suspend fun setReadingGoalMinutes(minutes: Int) = executeOperation(
        operationName = "setReadingGoalMinutes",
        parameters = mapOf("minutes" to minutes),
    ) {
        dataStore.edit { it[PreferencesKeys.READING_GOAL_MINUTES] = minutes }
    }

    suspend fun setDictionaryHistoryEnabled(enabled: Boolean) = executeOperation(
        operationName = "setDictionaryHistoryEnabled",
        parameters = mapOf("enabled" to enabled),
    ) {
        dataStore.edit { it[PreferencesKeys.DICTIONARY_HISTORY_ENABLED] = enabled }
    }

    suspend fun setDictionaryHistoryRetentionDays(days: Int) = executeOperation(
        operationName = "setDictionaryHistoryRetentionDays",
        parameters = mapOf("days" to days),
    ) {
        dataStore.edit { it[PreferencesKeys.DICTIONARY_HISTORY_RETENTION_DAYS] = days }
    }

    suspend fun setSmartRecognitionInfoDismissed(dismissed: Boolean) = executeOperation(
        operationName = "setSmartRecognitionInfoDismissed",
        parameters = mapOf("dismissed" to dismissed),
    ) {
        dataStore.edit { it[PreferencesKeys.SMART_RECOGNITION_INFO_DISMISSED] = dismissed }
    }

    suspend fun setAutoCollectionsEnabled(enabled: Boolean) = executeOperation(
        operationName = "setAutoCollectionsEnabled",
        parameters = mapOf("enabled" to enabled),
    ) {
        dataStore.edit { it[PreferencesKeys.AUTO_COLLECTIONS_ENABLED] = enabled }
    }

    suspend fun resetReaderSettings() = executeOperation("resetReaderSettings") {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.READER_THEME)
            preferences.remove(PreferencesKeys.FONT_SIZE)
            preferences.remove(PreferencesKeys.PAGE_SCROLL)
            preferences.remove(PreferencesKeys.CUSTOMIZE_LAYOUT)
            preferences.remove(PreferencesKeys.LINE_SPACING)
            preferences.remove(PreferencesKeys.CHAR_SPACING)
            preferences.remove(PreferencesKeys.WORD_SPACING)
            preferences.remove(PreferencesKeys.MARGINS)
            preferences.remove(PreferencesKeys.COLUMN_COUNT)
            preferences.remove(PreferencesKeys.JUSTIFY_TEXT)
        }
    }
}

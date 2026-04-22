package com.example.liber.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.liber.feature.library.LibrarySortOption
import com.example.liber.feature.library.LibraryViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    AUTO, DARK, LIGHT
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

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
        val DICTIONARY_HISTORY_RETENTION_DAYS = intPreferencesKey("dictionary_history_retention_days")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.AUTO.name
            ThemeMode.valueOf(themeName)
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    val readerTheme: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.READER_THEME] ?: "original" }

    suspend fun setReaderTheme(id: String) {
        context.dataStore.edit { it[PreferencesKeys.READER_THEME] = id }
    }

    val fontSize: Flow<Float> = context.dataStore.data
        .map { it[PreferencesKeys.FONT_SIZE] ?: 1.0f }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { it[PreferencesKeys.FONT_SIZE] = size }
    }

    val pageScroll: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.PAGE_SCROLL] ?: false }

    suspend fun setPageScroll(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PAGE_SCROLL] = enabled }
    }

    val customizeLayout: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.CUSTOMIZE_LAYOUT] ?: false }

    suspend fun setCustomizeLayout(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CUSTOMIZE_LAYOUT] = enabled }
    }

    val lineSpacing: Flow<Float> = context.dataStore.data
        .map { it[PreferencesKeys.LINE_SPACING] ?: 1.4f }

    suspend fun setLineSpacing(value: Float) {
        context.dataStore.edit { it[PreferencesKeys.LINE_SPACING] = value }
    }

    val charSpacing: Flow<Float> = context.dataStore.data
        .map { it[PreferencesKeys.CHAR_SPACING] ?: 0.0f }

    suspend fun setCharSpacing(value: Float) {
        context.dataStore.edit { it[PreferencesKeys.CHAR_SPACING] = value }
    }

    val wordSpacing: Flow<Float> = context.dataStore.data
        .map { it[PreferencesKeys.WORD_SPACING] ?: 0.0f }

    suspend fun setWordSpacing(value: Float) {
        context.dataStore.edit { it[PreferencesKeys.WORD_SPACING] = value }
    }

    val margins: Flow<Float> = context.dataStore.data
        .map { it[PreferencesKeys.MARGINS] ?: 24.0f }

    suspend fun setMargins(value: Float) {
        context.dataStore.edit { it[PreferencesKeys.MARGINS] = value }
    }

    val columnCount: Flow<String> = context.dataStore.data
        .map { it[PreferencesKeys.COLUMN_COUNT] ?: "auto" }

    suspend fun setColumnCount(value: String) {
        context.dataStore.edit { it[PreferencesKeys.COLUMN_COUNT] = value }
    }

    val justifyText: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.JUSTIFY_TEXT] ?: false }

    suspend fun setJustifyText(value: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.JUSTIFY_TEXT] = value }
    }

    val booksViewMode: Flow<LibraryViewMode> = context.dataStore.data
        .map { preferences ->
            val name = preferences[PreferencesKeys.BOOKS_VIEW_MODE] ?: LibraryViewMode.GRID.name
            LibraryViewMode.valueOf(name)
        }

    suspend fun setBooksViewMode(mode: LibraryViewMode) {
        context.dataStore.edit { it[PreferencesKeys.BOOKS_VIEW_MODE] = mode.name }
    }

    val booksSortOption: Flow<LibrarySortOption> = context.dataStore.data
        .map { preferences ->
            val name =
                preferences[PreferencesKeys.BOOKS_SORT_OPTION] ?: LibrarySortOption.RECENT.name
            LibrarySortOption.valueOf(name)
        }

    suspend fun setBooksSortOption(option: LibrarySortOption) {
        context.dataStore.edit { it[PreferencesKeys.BOOKS_SORT_OPTION] = option.name }
    }

    val audiobooksViewMode: Flow<LibraryViewMode> = context.dataStore.data
        .map { preferences ->
            val name =
                preferences[PreferencesKeys.AUDIOBOOKS_VIEW_MODE] ?: LibraryViewMode.GRID.name
            LibraryViewMode.valueOf(name)
        }

    suspend fun setAudiobooksViewMode(mode: LibraryViewMode) {
        context.dataStore.edit { it[PreferencesKeys.AUDIOBOOKS_VIEW_MODE] = mode.name }
    }

    val audiobooksSortOption: Flow<LibrarySortOption> = context.dataStore.data
        .map { preferences ->
            val name =
                preferences[PreferencesKeys.AUDIOBOOKS_SORT_OPTION] ?: LibrarySortOption.RECENT.name
            LibrarySortOption.valueOf(name)
        }

    val dictionaryHistoryEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.DICTIONARY_HISTORY_ENABLED] ?: true }

    val dictionaryHistoryRetentionDays: Flow<Int> = context.dataStore.data
        .map { it[PreferencesKeys.DICTIONARY_HISTORY_RETENTION_DAYS] ?: 90 }

    val readingGoalMinutes: Flow<Int> = context.dataStore.data
        .map { it[PreferencesKeys.READING_GOAL_MINUTES] ?: 30 }

    suspend fun setAudiobooksSortOption(option: LibrarySortOption) {
        context.dataStore.edit { it[PreferencesKeys.AUDIOBOOKS_SORT_OPTION] = option.name }
    }

    suspend fun setReadingGoalMinutes(minutes: Int) {
        context.dataStore.edit { it[PreferencesKeys.READING_GOAL_MINUTES] = minutes }
    }

    suspend fun setDictionaryHistoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DICTIONARY_HISTORY_ENABLED] = enabled }
    }

    suspend fun setDictionaryHistoryRetentionDays(days: Int) {
        context.dataStore.edit { it[PreferencesKeys.DICTIONARY_HISTORY_RETENTION_DAYS] = days }
    }

    suspend fun resetReaderSettings() {
        context.dataStore.edit { preferences ->
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

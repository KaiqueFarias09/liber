package com.example.liber.feature.settings

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewModelScope
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseAndroidViewModel
import com.example.liber.data.repository.ThemeMode
import com.example.liber.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val repository: UserPreferencesRepository,
    appLogger: AppLogger,
) : BaseAndroidViewModel(application, "SettingsViewModel", appLogger) {

    val supportedLanguages = listOf(
        LanguageOptions("en", "English"),
        LanguageOptions("pt-BR", "Português (Brasil)"),
        LanguageOptions("es-419", "Español (Latinoamérica)")
    )

    private val _currentLanguage = MutableStateFlow(getCurrentLanguageCode())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private fun getCurrentLanguageCode(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) locales.get(0)?.toLanguageTag() ?: "en" else "en"
    }

    fun setLanguage(languageTag: String) {
        _currentLanguage.value = languageTag

        val localeList = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.AUTO
        )

    fun setThemeMode(mode: ThemeMode) {
        launchSafely(
            actionName = "setThemeMode",
            parameters = mapOf("mode" to mode.name),
        ) {
            repository.setThemeMode(mode)
        }
    }
}

data class LanguageOptions(val tag: String, val displayName: String)

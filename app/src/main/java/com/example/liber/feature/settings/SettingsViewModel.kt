package com.example.liber.feature.settings

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewModelScope
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseAndroidViewModel
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.repository.AccentColor
import com.example.liber.data.repository.BackupRepository
import com.example.liber.data.repository.ThemeMode
import com.example.liber.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val repository: UserPreferencesRepository,
    private val backupRepository: BackupRepository,
    appLogger: AppLogger,
) : BaseAndroidViewModel(application, "SettingsViewModel", appLogger) {

    val supportedLanguages = listOf(
        LanguageOptions("en", "English"),
        LanguageOptions("pt-BR", "Português (Brasil)"),
        LanguageOptions("es-419", "Español (Latinoamérica)")
    )

    private val _currentLanguage = MutableStateFlow(getCurrentLanguageCode())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _backupState = MutableStateFlow<UiState<Unit>>(UiState.Success(Unit))
    val backupState: StateFlow<UiState<Unit>> = _backupState.asStateFlow()

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

    val accentColor: StateFlow<AccentColor> = repository.accentColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccentColor.ROSE
        )

    fun setAccentColor(accentColor: AccentColor) {
        launchSafely(
            actionName = "setAccentColor",
            parameters = mapOf("accentColor" to accentColor.name),
        ) {
            repository.setAccentColor(accentColor)
        }
    }

    fun exportBackup(onJsonReady: (String) -> Unit) {
        _backupState.value = UiState.Loading
        launchSafely(
            actionName = "exportBackup",
            onError = {
                _backupState.value =
                    UiState.Error(UiText.DynamicString(it.message ?: "Export failed"))
            }
        ) {
            val json = backupRepository.createBackupJson()
            _backupState.value = UiState.Success(Unit)
            onJsonReady(json)
        }
    }

    fun importBackup(jsonString: String) {
        _backupState.value = UiState.Loading
        launchSafely(
            actionName = "importBackup",
            onError = {
                _backupState.value =
                    UiState.Error(UiText.DynamicString(it.message ?: "Import failed"))
            }
        ) {
            backupRepository.restoreFromBackupJson(jsonString)
            _backupState.value = UiState.Success(Unit)
        }
    }
}

data class LanguageOptions(val tag: String, val displayName: String)

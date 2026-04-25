package com.example.liber.core.util

import com.example.liber.R

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(
        val message: UiText,
        val title: UiText = UiText.StringResource(R.string.error_default_title),
    ) : UiState<Nothing>
}

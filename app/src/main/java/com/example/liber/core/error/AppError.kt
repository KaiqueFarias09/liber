package com.example.liber.core.error

import com.example.liber.R
import com.example.liber.core.util.UiText
import com.example.liber.core.util.UiState
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

sealed class AppError(
    val title: UiText,
    val message: UiText,
) {
    data object Network : AppError(
        title = UiText.StringResource(R.string.error_network_title),
        message = UiText.StringResource(R.string.error_network_message),
    )

    data object Timeout : AppError(
        title = UiText.StringResource(R.string.error_timeout_title),
        message = UiText.StringResource(R.string.error_timeout_message),
    )

    data object NotFound : AppError(
        title = UiText.StringResource(R.string.error_not_found_title),
        message = UiText.StringResource(R.string.error_not_found_message),
    )

    data object PermissionDenied : AppError(
        title = UiText.StringResource(R.string.error_permission_title),
        message = UiText.StringResource(R.string.error_permission_message),
    )

    data object InvalidInput : AppError(
        title = UiText.StringResource(R.string.error_invalid_input_title),
        message = UiText.StringResource(R.string.error_invalid_input_message),
    )

    data object Server : AppError(
        title = UiText.StringResource(R.string.error_server_title),
        message = UiText.StringResource(R.string.error_server_message),
    )

    data object Unknown : AppError(
        title = UiText.StringResource(R.string.error_unknown_title),
        message = UiText.StringResource(R.string.error_unknown_message),
    )
}

fun Throwable.toAppError(): AppError = when (this) {
    is UnknownHostException -> AppError.Network
    is SocketTimeoutException -> AppError.Timeout
    is FileNotFoundException -> AppError.NotFound
    is SecurityException -> AppError.PermissionDenied
    is IllegalArgumentException -> AppError.InvalidInput
    is HttpException -> when (code()) {
        400, 422 -> AppError.InvalidInput
        401, 403 -> AppError.PermissionDenied
        404 -> AppError.NotFound
        in 500..599 -> AppError.Server
        else -> AppError.Unknown
    }
    is IOException -> AppError.Network
    else -> AppError.Unknown
}

fun AppError.toUiStateError(): UiState.Error = UiState.Error(
    message = message,
    title = title,
)

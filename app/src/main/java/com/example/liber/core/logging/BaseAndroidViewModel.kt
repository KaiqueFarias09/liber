package com.example.liber.core.logging

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.liber.core.util.rethrowIfCancellation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseAndroidViewModel(
    application: Application,
    viewModelName: String,
    appLogger: AppLogger,
) : AndroidViewModel(application) {
    protected val logger = ViewModelLogger(viewModelName, appLogger)

    protected fun launchSafely(
        actionName: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        parameters: Map<String, Any?> = emptyMap(),
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        logger.logActionStart(actionName, parameters)
        return viewModelScope.launch(dispatcher) {
            try {
                block()
                logger.logActionSuccess(actionName)
            } catch (throwable: Throwable) {
                throwable.rethrowIfCancellation()
                logger.recordError(throwable, actionName)
                onError(throwable)
            }
        }
    }
}

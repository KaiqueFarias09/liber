package com.example.liber.core.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liber.core.util.rethrowIfCancellation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseViewModel(
    viewModelName: String,
    appLogger: AppLogger,
) : ViewModel() {
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

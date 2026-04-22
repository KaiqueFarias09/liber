package com.example.liber.core.logging

class ViewModelLogger(
    private val viewModelName: String,
    private val appLogger: AppLogger,
) {
    fun log(message: String) {
        appLogger.debug("$viewModelName: $message", tag = viewModelName)
    }

    fun logActionStart(
        actionName: String,
        parameters: Map<String, Any?> = emptyMap(),
    ) {
        val parametersInfo = parameters
            .filterValues { it != null }
            .takeIf { it.isNotEmpty() }
            ?.entries
            ?.joinToString(prefix = " params={", postfix = "}") { (key, value) ->
                "$key=$value"
            }
            .orEmpty()

        appLogger.debug("Running $actionName$parametersInfo", tag = viewModelName)
    }

    fun logActionSuccess(actionName: String) {
        appLogger.debug("Action \"$actionName\" succeeded", tag = viewModelName)
    }

    fun recordError(
        throwable: Throwable,
        actionName: String,
        message: String? = null,
    ) {
        val reason = message ?: "Failure in $viewModelName action: $actionName"
        appLogger.error(reason, tag = viewModelName, throwable = throwable)
    }
}

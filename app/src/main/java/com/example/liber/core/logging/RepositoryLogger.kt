package com.example.liber.core.logging

class RepositoryLogger(
    private val repositoryName: String,
    private val appLogger: AppLogger,
) {
    fun log(message: String) {
        appLogger.debug("$repositoryName: $message", tag = repositoryName)
    }

    fun logOperationStart(
        operationName: String,
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

        appLogger.debug("Executing $operationName$parametersInfo", tag = repositoryName)
    }

    fun logOperationSuccess(operationName: String) {
        appLogger.debug("Operation \"$operationName\" succeeded", tag = repositoryName)
    }

    fun recordError(
        throwable: Throwable,
        operationName: String,
        message: String? = null,
    ) {
        val reason = message ?: "Failure in $repositoryName operation: $operationName"
        appLogger.error(reason, tag = repositoryName, throwable = throwable)
    }
}

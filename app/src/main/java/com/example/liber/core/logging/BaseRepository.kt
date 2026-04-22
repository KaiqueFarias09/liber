package com.example.liber.core.logging

import com.example.liber.core.util.rethrowIfCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart

abstract class BaseRepository(
    repositoryName: String,
    appLogger: AppLogger,
) {
    protected val logger = RepositoryLogger(repositoryName, appLogger)

    protected suspend inline fun <T> executeOperation(
        operationName: String,
        parameters: Map<String, Any?> = emptyMap(),
        operation: suspend () -> T,
    ): T {
        logger.logOperationStart(operationName, parameters)
        return try {
            operation().also {
                logger.logOperationSuccess(operationName)
            }
        } catch (throwable: Throwable) {
            throwable.rethrowIfCancellation()
            logger.recordError(throwable, operationName)
            throw throwable
        }
    }

    protected inline fun <T> executeQuery(
        operationName: String,
        parameters: Map<String, Any?> = emptyMap(),
        block: () -> T,
    ): T {
        logger.logOperationStart(operationName, parameters)
        return try {
            block().also {
                logger.logOperationSuccess(operationName)
            }
        } catch (throwable: Throwable) {
            throwable.rethrowIfCancellation()
            logger.recordError(throwable, operationName)
            throw throwable
        }
    }

    protected fun <T> observeOperation(
        operationName: String,
        parameters: Map<String, Any?> = emptyMap(),
        upstream: Flow<T>,
    ): Flow<T> {
        return upstream
            .onStart {
                logger.logOperationStart(operationName, parameters)
            }
            .catch { throwable ->
                throwable.rethrowIfCancellation()
                logger.recordError(throwable, operationName)
                throw throwable
            }
    }
}

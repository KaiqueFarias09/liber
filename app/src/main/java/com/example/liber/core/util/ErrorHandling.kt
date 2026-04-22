package com.example.liber.core.util

import kotlinx.coroutines.CancellationException

fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}

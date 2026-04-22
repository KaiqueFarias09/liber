package com.example.liber.core.logging

interface AppLogger {
    fun debug(message: String, tag: String? = null)

    fun info(message: String, tag: String? = null)

    fun warn(message: String, tag: String? = null, throwable: Throwable? = null)

    fun error(message: String, tag: String? = null, throwable: Throwable? = null)
}

package com.example.liber.core.logging

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppLogger @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AppLogger {

    override fun debug(message: String, tag: String?) {
        if (!isDebuggable) return
        Log.d(resolveTag(tag), message)
    }

    override fun info(message: String, tag: String?) {
        if (!isDebuggable) return
        Log.i(resolveTag(tag), message)
    }

    override fun warn(message: String, tag: String?, throwable: Throwable?) {
        if (!isDebuggable) return
        Log.w(resolveTag(tag), message, throwable)
    }

    override fun error(message: String, tag: String?, throwable: Throwable?) {
        if (!isDebuggable) return
        Log.e(resolveTag(tag), message, throwable)
    }

    private fun resolveTag(tag: String?): String = tag ?: DEFAULT_TAG

    private val isDebuggable: Boolean
        get() = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private companion object {
        const val DEFAULT_TAG = "Liber"
    }
}

package com.example.liber.feature.reader.engine

import android.content.Context
import android.os.Build
import com.example.liber.core.logging.AndroidAppLogger
import com.example.liber.feature.reader.engine.CREngine.init
import com.example.liber.feature.reader.engine.CREngine.uninit
import org.coolreader.crengine.Engine
import java.io.File

/**
 * Application-level singleton that manages CREngine lifecycle.
 *
 * Call [init] once (e.g., in Application.onCreate or before the first reader
 * opens). Call [uninit] when the process is shutting down (optional — the OS
 * reclaims memory anyway).
 */
object CREngine {

    @Volatile
    private var initialized = false

    /**
     * Loads the native .so and initialises font manager + document cache.
     * Safe to call multiple times; subsequent calls are no-ops.
     */
    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            System.loadLibrary("cr3engine")

            // Collect system fonts available on this device
            val fontDirs = listOf(
                "/system/fonts",
                "/system/font",
                "/data/fonts"
            )
            val fonts = mutableListOf<String>()
            for (dir in fontDirs) {
                File(dir).listFiles()?.forEach { f ->
                    if (f.isFile && (f.name.endsWith(".ttf") || f.name.endsWith(".otf"))) {
                        fonts.add(f.absolutePath)
                    }
                }
            }

            val sdkInt = Build.VERSION.SDK_INT
            val ok = Engine.init(fonts.toTypedArray(), sdkInt)
            if (!ok) {
                android.util.Log.w("CREngine", "Engine.init returned false - no fonts registered")
            }

            // Set up a document cache (~128 MB) in the app's cache directory
            val cacheDir = File(context.cacheDir, "cr3cache")
            cacheDir.mkdirs()
            Engine.setCacheDirectory(cacheDir.absolutePath, 128)

            initialized = true
        }
    }

    fun uninit() {
        if (!initialized) return
        Engine.uninit()
        initialized = false
    }

    val isInitialized: Boolean get() = initialized
}

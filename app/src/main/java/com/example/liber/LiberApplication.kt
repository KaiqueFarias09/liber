package com.example.liber

import android.app.Application
import android.os.StrictMode
import com.example.liber.feature.reader.engine.CREngine
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LiberApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build(),
            )
        }

        CREngine.init(this)
    }
}

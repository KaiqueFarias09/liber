package com.example.liber

import android.app.Application
import com.example.liber.feature.reader.engine.CREngine
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LiberApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CREngine.init(this)
    }
}

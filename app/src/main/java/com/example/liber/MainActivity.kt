package com.example.liber

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.example.liber.core.designsystem.LiberTheme
import com.example.liber.core.intent.AppIntentHandler
import com.example.liber.core.intent.IncomingIntentAction
import com.example.liber.core.logging.AppLogger
import com.example.liber.feature.home.HomeViewModel
import com.example.liber.feature.settings.SettingsViewModel
import com.example.liber.ui.LiberApp
import com.example.liber.ui.LiberAppViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var appIntentHandler: AppIntentHandler

    @Inject
    lateinit var appLogger: AppLogger

    private val homeViewModel: HomeViewModel by viewModels()
    private val liberAppViewModel: LiberAppViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val params = window.attributes

            runCatching {
                val fieldMin = params.javaClass.getField("preferredMinDisplayRefreshRate")
                val fieldMax = params.javaClass.getField("preferredMaxDisplayRefreshRate")
                fieldMin.setFloat(params, 0f)
                fieldMax.setFloat(params, 0f)
                window.attributes = params
            }
        }

        handleIntent(intent)
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val accentColor by settingsViewModel.accentColor.collectAsState()

            LiberTheme(
                themeMode = themeMode,
                accentColor = accentColor
            ) {
                val windowSizeClass = calculateWindowSizeClass(this)
                LiberApp(
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        lifecycleScope.launch {
            runCatching {
                when (val result = appIntentHandler.resolveIncomingIntent(intent)) {
                    is IncomingIntentAction.OpenAudiobook ->
                        liberAppViewModel.openAudiobook(result.book)

                    is IncomingIntentAction.ImportAndOpen ->
                        homeViewModel.importAndOpenBook(result.uri, liberAppViewModel)

                    is IncomingIntentAction.Unhandled -> Unit
                }
            }.onFailure { exception ->
                // Catch SecurityException or IllegalArgumentException from malicious/bad intents
                appLogger.error(
                    "Failed to handle incoming intent action=${intent.action}",
                    tag = "MainActivity",
                    throwable = exception,
                )
            }
        }
    }
}

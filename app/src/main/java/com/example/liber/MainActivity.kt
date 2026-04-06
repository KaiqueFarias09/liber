package com.example.liber

import android.content.Intent
import android.net.Uri
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
import com.example.liber.ui.LiberApp
import com.example.liber.ui.LiberAppViewModel
import com.example.liber.ui.collections.CollectionsViewModel
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.settings.SettingsViewModel
import com.example.liber.ui.theme.LiberTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private val collectionsViewModel: CollectionsViewModel by viewModels()
    private val liberAppViewModel: LiberAppViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()

            LiberTheme(themeMode = themeMode) {
                val windowSizeClass = calculateWindowSizeClass(this)
                LiberApp(
                    viewModel = viewModel,
                    collectionsViewModel = collectionsViewModel,
                    liberAppViewModel = liberAppViewModel,
                    settingsViewModel = settingsViewModel,
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
        val action = intent?.action
        val type = intent?.type

        if (Intent.ACTION_VIEW == action) {
            intent.data?.let { uri ->
                viewModel.importAndOpenBook(uri, liberAppViewModel)
            }
        } else if (Intent.ACTION_SEND == action && type != null) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            uri?.let { viewModel.importAndOpenBook(it, liberAppViewModel) }
        }
    }
}

package com.example.liber

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.liber.ui.LiberApp
import com.example.liber.ui.collections.CollectionsViewModel
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.theme.LiberTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private val collectionsViewModel: CollectionsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiberTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                LiberApp(
                    viewModel = viewModel,
                    collectionsViewModel = collectionsViewModel,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }
}

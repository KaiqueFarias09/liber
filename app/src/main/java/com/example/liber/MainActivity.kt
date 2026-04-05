package com.example.liber

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.liber.ui.LiberApp
import com.example.liber.ui.collections.CollectionsViewModel
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.theme.LiberTheme

class MainActivity : FragmentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private val collectionsViewModel: CollectionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiberTheme {
                LiberApp(viewModel = viewModel, collectionsViewModel = collectionsViewModel)
            }
        }
    }
}

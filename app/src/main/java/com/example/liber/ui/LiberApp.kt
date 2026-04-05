package com.example.liber.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.liber.ui.components.LiberBottomNav
import com.example.liber.ui.home.HomeScreen
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.library.LibraryScreen
import com.example.liber.ui.navigation.AppTab
import com.example.liber.ui.reader.ReaderScreen
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication

/**
 * Root composable that owns app-level navigation state:
 * - which [AppTab] is active
 * - whether a [Publication] is currently open in the reader
 *
 * All data fetching and mutations are delegated to [HomeViewModel];
 * screens receive only plain data and callbacks.
 */
@OptIn(ExperimentalReadiumApi::class)
@Composable
fun LiberApp(viewModel: HomeViewModel) {
    var activePublication by remember { mutableStateOf<Publication?>(null) }
    var activeBook by remember { mutableStateOf<com.example.liber.data.Book?>(null) }
    var activeTab by remember { mutableStateOf(AppTab.HOME) }
    val scope = rememberCoroutineScope()

    val bookLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.loadBooksFromUris(uris)
    }

    val onOpenBook: (com.example.liber.data.Book) -> Unit = { book ->
        scope.launch {
            activeBook = book
            activePublication = viewModel.openBook(book)
        }
    }

    if (activePublication != null) {
        ReaderScreen(
            publication = activePublication!!,
            bookId = activeBook!!.id,
            initialLocatorJson = activeBook?.lastLocator,
            onSaveLocator = { json, progress -> viewModel.saveLocator(activeBook!!.id, json, progress) },
            onBack = {
                activePublication = null
                activeBook = null
            },
        )
    } else {
        Scaffold(
            containerColor = Color(0xFF111111),
            contentColor = Color(0xFFF2F2F7),
            bottomBar = {
                LiberBottomNav(
                    activeTab = activeTab,
                    onTabChange = { activeTab = it },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                when (activeTab) {
                    AppTab.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onBookClick = onOpenBook,
                    )
                    AppTab.LIBRARY -> LibraryScreen(
                        viewModel = viewModel,
                        onBookClick = onOpenBook,
                        onAddBooks = { bookLauncher.launch(arrayOf("application/epub+zip")) },
                    )
                }
            }
        }
    }
}

package com.example.liber.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.liber.data.AnnotationEntity
import com.example.liber.ui.reader.AnnotationRequest
import com.example.liber.ui.components.LiberBottomNav
import com.example.liber.ui.home.HomeScreen
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.library.LibraryScreen
import com.example.liber.ui.navigation.AppTab
import com.example.liber.ui.reader.ReaderScreen
import kotlinx.coroutines.flow.emptyFlow
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

    // Collect annotations for the currently open book
    val annotationsFlow = remember(activeBook?.id) {
        activeBook?.id?.let { viewModel.getAnnotationsForBook(it) } ?: emptyFlow()
    }
    val annotations by annotationsFlow.collectAsState(initial = emptyList<AnnotationEntity>())

    // Posted by MainActivity when the user selects text and taps Highlight / Add Note
    val pendingAnnotationRequest by viewModel.pendingAnnotationRequest.collectAsState()

    if (activePublication != null) {
        ReaderScreen(
            publication = activePublication!!,
            bookId = activeBook!!.id,
            initialLocatorJson = activeBook?.lastLocator,
            annotations = annotations,
            pendingAnnotationRequest = pendingAnnotationRequest,
            onSaveLocator = { json, progress -> viewModel.saveLocator(activeBook!!.id, json, progress) },
            onSaveAnnotation = { annotation -> viewModel.saveAnnotation(annotation) },
            onDeleteAnnotation = { annotationId -> viewModel.deleteAnnotation(annotationId) },
            onClearPendingAnnotation = { viewModel.clearPendingAnnotation() },
            onBack = {
                activePublication = null
                activeBook = null
            },
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
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

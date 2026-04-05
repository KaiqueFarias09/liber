package com.example.liber.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.liber.data.AnnotationEntity
import com.example.liber.data.BookmarkEntity
import com.example.liber.ui.collections.CollectionsViewModel
import com.example.liber.ui.components.LiberBottomNav
import com.example.liber.ui.components.LiberNavRail
import com.example.liber.ui.home.HomeScreen
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.library.LibraryScreen
import com.example.liber.ui.navigation.AppTab
import com.example.liber.ui.reader.PdfReaderScreen
import com.example.liber.ui.reader.ReaderScreen
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Root composable for app-level navigation.
 */
@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LiberApp(
    viewModel: HomeViewModel,
    collectionsViewModel: CollectionsViewModel,
    liberAppViewModel: LiberAppViewModel,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass
) {
    val context = LocalContext.current
    val showNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val scope = rememberCoroutineScope()

    val activeBook by liberAppViewModel.activeBook.collectAsState()
    val activePublication by liberAppViewModel.activePublication.collectAsState()
    val activeTab by liberAppViewModel.activeTab.collectAsState()

    val bookLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.loadBooksFromUris(uris)
    }

    val onOpenBook: (com.example.liber.data.Book) -> Unit = { book ->
        scope.launch {
            val publication = viewModel.openBook(book)
            if (publication != null) {
                liberAppViewModel.openEpub(book, publication)
            } else {
                liberAppViewModel.openPdf(book)
            }
        }
    }

    val annotationsFlow = remember(activeBook?.id) {
        activeBook?.id?.let { viewModel.getAnnotationsForBook(it) } ?: emptyFlow()
    }
    val annotations by annotationsFlow.collectAsState(initial = emptyList<AnnotationEntity>())

    val bookmarksFlow = remember(activeBook?.id) {
        activeBook?.id?.let { viewModel.getBookmarksForBook(it) } ?: emptyFlow()
    }
    val bookmarks by bookmarksFlow.collectAsState(initial = emptyList<BookmarkEntity>())

    val pendingAnnotationRequest by viewModel.pendingAnnotationRequest.collectAsState()

    val book = activeBook
    val publication = activePublication

    if (book != null && book.fileUri.toString().endsWith(".pdf", ignoreCase = true)) {
        PdfReaderScreen(
            uri = book.fileUri,
            title = book.title,
            onBack = { liberAppViewModel.closeReader() }
        )
    } else if (publication != null && book != null) {
        ReaderScreen(
            publication = publication,
            bookId = book.id,
            initialLocatorJson = book.lastLocator,
            annotations = annotations,
            bookmarks = bookmarks,
            pendingAnnotationRequest = pendingAnnotationRequest,
            onRequestAnnotation = { request -> viewModel.requestAnnotation(request) },
            onSaveLocator = { json, progress -> viewModel.saveLocator(book.id, json, progress) },
            onSaveAnnotation = { annotation -> viewModel.saveAnnotation(annotation) },
            onDeleteAnnotation = { annotationId -> viewModel.deleteAnnotation(annotationId) },
            onSaveBookmark = { bookmark -> viewModel.saveBookmark(bookmark) },
            onDeleteBookmark = { bookmarkId -> viewModel.deleteBookmark(bookmarkId) },
            onClearPendingAnnotation = { viewModel.clearPendingAnnotation() },
            onBack = { liberAppViewModel.closeReader() },
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                if (!showNavRail) {
                    LiberBottomNav(
                        activeTab = activeTab,
                        onTabChange = { liberAppViewModel.setActiveTab(it) },
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (showNavRail) {
                    LiberNavRail(
                        activeTab = activeTab,
                        onTabChange = { liberAppViewModel.setActiveTab(it) },
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxSize()
                    ) {
                        when (activeTab) {
                            AppTab.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onBookClick = onOpenBook,
                                modifier = Modifier.fillMaxSize()
                            )
                            AppTab.LIBRARY -> LibraryScreen(
                                viewModel = viewModel,
                                onBookClick = onOpenBook,
                                onAddBooks = {
                                    bookLauncher.launch(
                                        arrayOf(
                                            "application/epub+zip",
                                            "application/pdf",
                                            "application/x-cbz",
                                            "application/audiobook+zip",
                                            "application/lpf+zip",
                                            "application/webpub+json"
                                        )
                                    )
                                },
                                onShareBook = { book ->
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/epub+zip"
                                        putExtra(Intent.EXTRA_STREAM, book.fileUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Book"))
                                },
                                collectionsViewModel = collectionsViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

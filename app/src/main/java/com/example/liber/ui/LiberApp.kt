package com.example.liber.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.liber.data.AnnotationEntity
import com.example.liber.data.BookmarkEntity
import com.example.liber.ui.reader.AnnotationRequest
import com.example.liber.ui.collections.CollectionsScreen
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
import org.readium.r2.shared.publication.Publication

/**
 * Root composable that owns app-level navigation state.
 */
@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LiberApp(
    viewModel: HomeViewModel,
    collectionsViewModel: CollectionsViewModel,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass
) {
    val context = LocalContext.current
    val showNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

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

    val annotationsFlow = remember(activeBook?.id) {
        activeBook?.id?.let { viewModel.getAnnotationsForBook(it) } ?: emptyFlow()
    }
    val annotations by annotationsFlow.collectAsState(initial = emptyList<AnnotationEntity>())

    val bookmarksFlow = remember(activeBook?.id) {
        activeBook?.id?.let { viewModel.getBookmarksForBook(it) } ?: emptyFlow()
    }
    val bookmarks by bookmarksFlow.collectAsState(initial = emptyList<BookmarkEntity>())

    val pendingAnnotationRequest by viewModel.pendingAnnotationRequest.collectAsState()

    if (activeBook?.fileUri?.toString()?.endsWith(".pdf", ignoreCase = true) == true) {
        PdfReaderScreen(
            uri = activeBook!!.fileUri,
            title = activeBook!!.title,
            onBack = {
                activeBook = null
            }
        )
    } else if (activePublication != null) {
        ReaderScreen(
            publication = activePublication!!,
            bookId = activeBook!!.id,
            initialLocatorJson = activeBook?.lastLocator,
            annotations = annotations,
            bookmarks = bookmarks,
            pendingAnnotationRequest = pendingAnnotationRequest,
            onRequestAnnotation = { request -> viewModel.requestAnnotation(request) },
            onSaveLocator = { json, progress -> viewModel.saveLocator(activeBook!!.id, json, progress) },
            onSaveAnnotation = { annotation -> viewModel.saveAnnotation(annotation) },
            onDeleteAnnotation = { annotationId -> viewModel.deleteAnnotation(annotationId) },
            onSaveBookmark = { bookmark -> viewModel.saveBookmark(bookmark) },
            onDeleteBookmark = { bookmarkId -> viewModel.deleteBookmark(bookmarkId) },
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
                if (!showNavRail) {
                    LiberBottomNav(
                        activeTab = activeTab,
                        onTabChange = { activeTab = it },
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
                        onTabChange = { activeTab = it },
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    val books by viewModel.books.collectAsState()
                    when (activeTab) {
                        AppTab.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onBookClick = onOpenBook,
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
                        )
                        AppTab.COLLECTIONS -> CollectionsScreen(
                            viewModel = collectionsViewModel,
                            allBooks = books,
                            onOpenBook = onOpenBook,
                            onShareBook = { book ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/epub+zip"
                                    putExtra(Intent.EXTRA_STREAM, book.fileUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Book"))
                            },
                            onToggleWantToRead = { book -> viewModel.toggleWantToRead(book.id, book.wantToRead) },
                            onToggleFinished = { book -> viewModel.toggleFinished(book.id, book.readingProgress == 100) },
                            onRenameBook = { book, newTitle -> viewModel.renameBook(book.id, newTitle) },
                        )
                    }
                }
            }
        }
    }
}

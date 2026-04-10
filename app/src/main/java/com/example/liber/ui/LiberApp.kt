package com.example.liber.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.liber.data.AnnotationEntity
import com.example.liber.data.Book
import com.example.liber.data.BookmarkEntity
import com.example.liber.service.BookScanService
import com.example.liber.ui.collections.CollectionsViewModel
import com.example.liber.ui.components.LiberBottomNav
import com.example.liber.ui.components.LiberNavRail
import com.example.liber.ui.components.NowPlayingBar
import com.example.liber.ui.home.HomeScreen
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.library.LibraryScreen
import com.example.liber.ui.navigation.AppTab
import com.example.liber.ui.reader.AudioPlayerScreen
import com.example.liber.ui.reader.AudiobookPlayerViewModel
import com.example.liber.ui.reader.PdfReaderScreen
import com.example.liber.ui.reader.ReaderScreen
import com.example.liber.ui.settings.SettingsScreen
import com.example.liber.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocalizedString

/**
 * Root composable for app-level navigation.
 */
@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LiberApp(
    viewModel: HomeViewModel,
    collectionsViewModel: CollectionsViewModel,
    liberAppViewModel: LiberAppViewModel,
    settingsViewModel: SettingsViewModel,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass
) {
    val context = LocalContext.current
    val showNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val scope = rememberCoroutineScope()

    val audiobookPlayerViewModel: AudiobookPlayerViewModel = viewModel(
        factory = AudiobookPlayerViewModel.Factory(
            context.applicationContext as Application,
            viewModel.bookRepository
        )
    )

    val activeBook by liberAppViewModel.activeBook.collectAsState()
    val activePublication by liberAppViewModel.activePublication.collectAsState()
    val isReaderOpen by liberAppViewModel.isReaderOpen.collectAsState()
    val activeTab by liberAppViewModel.activeTab.collectAsState()
    val isPlayingGlobal by liberAppViewModel.isPlaying.collectAsState()
    val selectedCollectionId by liberAppViewModel.selectedCollectionId.collectAsState()
    val scanSources by viewModel.scanSources.collectAsState()

    // Sync global playing state to the player
    androidx.compose.runtime.LaunchedEffect(isPlayingGlobal) {
        if (isPlayingGlobal) {
            audiobookPlayerViewModel.play()
        } else {
            audiobookPlayerViewModel.pause()
        }
    }

    // Sync player state back to global state
    val playerIsPlaying by audiobookPlayerViewModel.isPlaying.collectAsState()
    val playerPositionMs by audiobookPlayerViewModel.positionMs.collectAsState()
    val playerDurationMs by audiobookPlayerViewModel.durationMs.collectAsState()

    androidx.compose.runtime.LaunchedEffect(playerIsPlaying) {
        if (playerIsPlaying != isPlayingGlobal) {
            liberAppViewModel.setPlaying(playerIsPlaying)
        }
    }

    androidx.compose.runtime.LaunchedEffect(playerPositionMs, playerDurationMs) {
        if (playerDurationMs > 0) {
            liberAppViewModel.setPlayerProgress(playerPositionMs.toFloat() / playerDurationMs.toFloat())
        }
    }

    androidx.activity.compose.BackHandler(enabled = selectedCollectionId != null) {
        liberAppViewModel.setSelectedCollectionId(null)
    }

    val bookLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.loadBooksFromUris(uris)
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* scan proceeds regardless of whether notification permission was granted */ }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        treeUri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val folderName = DocumentFile.fromTreeUri(context, treeUri)?.name ?: "Folder"
        viewModel.addScanSource(treeUri, folderName)
        val intent = BookScanService.buildIntent(context, treeUri, folderName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    val onScanFolder: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        folderLauncher.launch(null)
    }

    val onRescanFolder: (com.example.liber.data.ScanSourceEntity) -> Unit = { source ->
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val treeUri = android.net.Uri.parse(source.treeUri)
        val intent = BookScanService.buildIntent(context, treeUri, source.displayName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    val onOpenBook: (Book) -> Unit = { book ->
        scope.launch {
            val publication = viewModel.openBook(book)
            when {
                publication != null -> {
                    // This handles EPUBs and synthesized audiobook publications
                    liberAppViewModel.openEpub(book, publication)
                }

                book.mediaType == "audio/mpeg" || book.mediaType == "audiobook" -> {
                    // Use the publication if it was synthesized, otherwise just open the reader
                    if (publication != null) {
                        liberAppViewModel.openEpub(book, publication)
                    } else {
                        liberAppViewModel.openPdf(book)
                    }
                }

                else -> {
                    liberAppViewModel.openPdf(book)
                }
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
    val playerProgress by liberAppViewModel.playerProgress.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isReaderOpen && book != null) {
            if (book.fileUri.toString().endsWith(".pdf", ignoreCase = true)) {
                val initialPage = remember(book.lastLocator) {
                    book.lastLocator?.let {
                        runCatching { org.json.JSONObject(it).getInt("page") }.getOrDefault(0)
                    } ?: 0
                }
                PdfReaderScreen(
                    uri = book.fileUri,
                    title = book.title,
                    bookId = book.id,
                    initialPage = initialPage,
                    bookmarks = bookmarks,
                    annotations = annotations,
                    onSaveLocator = { json, progress ->
                        viewModel.saveLocator(
                            book.id,
                            json,
                            progress
                        )
                    },
                    onSaveBookmark = { bookmark -> viewModel.saveBookmark(bookmark) },
                    onDeleteBookmark = { bookmarkId -> viewModel.deleteBookmark(bookmarkId) },
                    onSaveAnnotation = { annotation -> viewModel.saveAnnotation(annotation) },
                    onDeleteAnnotation = { annotationId -> viewModel.deleteAnnotation(annotationId) },
                    onBack = { liberAppViewModel.closeReader() },
                )
            } else if (publication != null && (book.mediaType != "audio/mpeg" && book.mediaType != "audiobook")) {
                ReaderScreen(
                    publication = publication,
                    bookId = book.id,
                    initialLocatorJson = book.lastLocator,
                    annotations = annotations,
                    bookmarks = bookmarks,
                    pendingAnnotationRequest = pendingAnnotationRequest,
                    onRequestAnnotation = { request -> viewModel.requestAnnotation(request) },
                    onSaveLocator = { json, progress ->
                        viewModel.saveLocator(
                            book.id,
                            json,
                            progress
                        )
                    },
                    onSaveAnnotation = { annotation -> viewModel.saveAnnotation(annotation) },
                    onDeleteAnnotation = { annotationId -> viewModel.deleteAnnotation(annotationId) },
                    onSaveBookmark = { bookmark -> viewModel.saveBookmark(bookmark) },
                    onDeleteBookmark = { bookmarkId -> viewModel.deleteBookmark(bookmarkId) },
                    onClearPendingAnnotation = { viewModel.clearPendingAnnotation() },
                    onBack = { liberAppViewModel.closeReader() },
                )
            }
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
                                    liberAppViewModel = liberAppViewModel,
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
                                    onShareBook = { bookToShare: Book ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/epub+zip"
                                            putExtra(Intent.EXTRA_STREAM, bookToShare.fileUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                intent,
                                                "Share Book"
                                            )
                                        )
                                    },
                                    collectionsViewModel = collectionsViewModel,
                                    liberAppViewModel = liberAppViewModel,
                                    selectedCollectionId = selectedCollectionId,
                                    onCollectionClick = { id: Long? ->
                                        liberAppViewModel.setSelectedCollectionId(
                                            id
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                AppTab.SETTINGS -> SettingsScreen(
                                    viewModel = settingsViewModel,
                                    scanSources = scanSources,
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
                                    onAddScanFolder = onScanFolder,
                                    onRescanFolder = onRescanFolder,
                                    onRemoveFolder = { source -> viewModel.removeScanSource(source.treeUri) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overlay the NowPlayingBar and AudioPlayerScreen
        val isAudiobook =
            book != null && (book.mediaType == "audio/mpeg" || book.mediaType == "audiobook")

        if (isAudiobook) {
            // AudioPlayerScreen with slide animation
            AnimatedVisibility(
                visible = isReaderOpen,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 500)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 500)
                ) + fadeOut()
            ) {
                AudioPlayerScreen(
                    book = book!!,
                    publication = publication ?: org.readium.r2.shared.publication.Publication(
                        org.readium.r2.shared.publication.Manifest(
                            metadata = org.readium.r2.shared.publication.Metadata(
                                localizedTitle = LocalizedString(book.title)
                            )
                        )
                    ),
                    liberAppViewModel = liberAppViewModel,
                    homeViewModel = viewModel,
                    audiobookPlayerViewModel = audiobookPlayerViewModel,
                    onBack = { liberAppViewModel.closeReader() },
                    onSaveLocator = { json, progress ->
                        viewModel.saveLocator(
                            book.id,
                            json,
                            progress
                        )
                    }
                )
            }

            // NowPlayingBar with fade animation
            AnimatedVisibility(
                visible = !isReaderOpen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showNavRail) 16.dp else 80.dp) // Adjusted for BottomNav
                    .navigationBarsPadding()
            ) {
                NowPlayingBar(
                    book = book!!,
                    isPlaying = isPlayingGlobal,
                    progress = playerProgress,
                    onTogglePlay = {
                        if (book.isAudiobook) {
                            audiobookPlayerViewModel.togglePlayPause()
                        } else {
                            liberAppViewModel.setPlaying(!isPlayingGlobal)
                        }
                    },
                    onRewind = {
                        if (book.isAudiobook) {
                            audiobookPlayerViewModel.skipBackward(15)
                        } else {
                            liberAppViewModel.seekBy(-15)
                        }
                    },
                    onForward = {
                        if (book.isAudiobook) {
                            audiobookPlayerViewModel.skipForward(15)
                        } else {
                            liberAppViewModel.seekBy(15)
                        }
                    },
                    onClick = {
                        liberAppViewModel.openEpub(book, publication!!)
                    }
                )
            }
        }
    }
}

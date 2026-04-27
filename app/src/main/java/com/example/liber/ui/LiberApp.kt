package com.example.liber.ui

import android.Manifest
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.liber.core.designsystem.LiberBottomNav
import com.example.liber.core.designsystem.LiberNavRail
import com.example.liber.core.designsystem.responsiveMaxWidth
import com.example.liber.core.navigation.AppNavHost
import com.example.liber.core.navigation.AppRoute
import com.example.liber.core.navigation.AppTab
import com.example.liber.data.model.BookPreview
import com.example.liber.data.model.ScanSource
import com.example.liber.feature.audiobook.AudioPlayerScreen
import com.example.liber.feature.audiobook.AudiobookPlayerViewModel
import com.example.liber.feature.audiobook.components.NowPlayingBar
import com.example.liber.feature.collections.CollectionsViewModel
import com.example.liber.feature.dictionary.DictionaryViewModel
import com.example.liber.feature.home.HomeViewModel
import com.example.liber.feature.reader.ReaderScreen
import com.example.liber.feature.settings.SettingsViewModel
import com.example.liber.service.BookScanService
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

/**
 * Root composable for app-level navigation.
 * ViewModels are obtained via Hilt — no manual passing from MainActivity.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LiberApp(
    windowSizeClass: WindowSizeClass,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showNavRail = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    // ── ViewModels via Hilt ──────────────────────────────────────────────────
    val homeViewModel: HomeViewModel = hiltViewModel()
    val collectionsViewModel: CollectionsViewModel = hiltViewModel()
    val liberAppViewModel: LiberAppViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val dictionaryViewModel: DictionaryViewModel = hiltViewModel()
    val audiobookPlayerViewModel: AudiobookPlayerViewModel = hiltViewModel()
    val userPreferencesRepository = liberAppViewModel.userPreferencesRepository

    // ── Navigation ───────────────────────────────────────────────────────────
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isTopLevelRoute = currentRoute in listOf(AppRoute.HOME, AppRoute.LIBRARY, AppRoute.SETTINGS)

    val activeTab = when (currentRoute) {
        AppRoute.LIBRARY -> AppTab.LIBRARY
        AppRoute.SETTINGS -> AppTab.SETTINGS
        else -> AppTab.HOME
    }

    val onTabChange: (AppTab) -> Unit = { tab ->
        val route = when (tab) {
            AppTab.HOME -> AppRoute.HOME
            AppTab.LIBRARY -> AppRoute.LIBRARY
            AppTab.SETTINGS -> AppRoute.SETTINGS
        }
        navController.navigate(route) {
            popUpTo(AppRoute.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // ── App state ────────────────────────────────────────────────────────────
    val activeBook by liberAppViewModel.activeBook.collectAsState()
    val allBooks by homeViewModel.books.collectAsState()

    val isReaderOpen by liberAppViewModel.isReaderOpen.collectAsState()
    val book = remember(activeBook?.id, allBooks, isReaderOpen) {
        // activeBook in LiberAppViewModel is already a full Book.
        // We always use that for immersive screens.
        activeBook
    }
    val isPlayingGlobal by liberAppViewModel.isPlaying.collectAsState()
    val playWhenReadyGlobal by liberAppViewModel.playWhenReady.collectAsState()

    // ── Audiobook player state sync ──────────────────────────────────────────
    val playerIsPlaying by audiobookPlayerViewModel.isPlaying.collectAsState()
    val playerPlayWhenReady by audiobookPlayerViewModel.playWhenReady.collectAsState()
    val playerPositionMs by audiobookPlayerViewModel.positionMs.collectAsState()
    val playerDurationMs by audiobookPlayerViewModel.durationMs.collectAsState()

    LaunchedEffect(book?.id) {
        val currentBook = book
        if (currentBook != null && currentBook.isAudiobook) {
            audiobookPlayerViewModel.loadBook(currentBook)
        }
    }

    LaunchedEffect(book?.title, book?.author, book?.coverUri, book?.narrator) {
        val currentBook = book
        if (currentBook != null && currentBook.isAudiobook) {
            audiobookPlayerViewModel.updateMetadataIfLoaded(currentBook)
        }
    }

    LaunchedEffect(playerIsPlaying) {
        if (playerIsPlaying != isPlayingGlobal) {
            liberAppViewModel.setPlaying(playerIsPlaying)
        }
    }

    LaunchedEffect(playerPlayWhenReady) {
        if (playerPlayWhenReady != playWhenReadyGlobal) {
            liberAppViewModel.setPlayWhenReady(playerPlayWhenReady)
        }
    }

    LaunchedEffect(playerDurationMs, playerPositionMs) {
        if (playerDurationMs > 0) {
            liberAppViewModel.setPlayerProgress(playerPositionMs.toFloat() / playerDurationMs.toFloat())
        }
    }

    LaunchedEffect(book?.id, isReaderOpen) {
        val activeBook = book
        if (activeBook != null && isReaderOpen && !activeBook.isAudiobook) {
            liberAppViewModel.startReaderSession(activeBook.id)
        } else {
            liberAppViewModel.stopReaderSession()
        }
    }

    androidx.activity.compose.BackHandler(enabled = isReaderOpen) {
        if (isReaderOpen) {
            liberAppViewModel.closeReader()
        }
    }

    // ── Activity result launchers ────────────────────────────────────────────
    val bookLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) homeViewModel.loadBooksFromUris(uris)
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* scan proceeds regardless */ }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        treeUri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val folderName = DocumentFile.fromTreeUri(context, treeUri)?.name ?: "Folder"
        homeViewModel.addScanSource(treeUri, folderName)
        val intent = BookScanService.buildIntent(context, treeUri, folderName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    val onAddBooks: () -> Unit = {
        bookLauncher.launch(
            arrayOf(
                "application/epub+zip",
                "application/x-cbz",
                "application/audiobook+zip",
                "application/lpf+zip",
                "application/webpub+json"
            )
        )
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

    val onRescanFolder: (ScanSource) -> Unit = { source ->
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val treeUri = source.treeUri.toUri()
        val intent = BookScanService.buildIntent(context, treeUri, source.displayName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    val onOpenBook: (BookPreview) -> Unit = { previewToOpen ->
        if (previewToOpen.isAudiobook) {
            liberAppViewModel.openAudiobook(previewToOpen)
            scope.launch {
                val fullBook = homeViewModel.bookRepository.getBookById(previewToOpen.id)
                fullBook?.let { homeViewModel.openBook(it) }
            }
        } else {
            scope.launch {
                val fullBook = homeViewModel.bookRepository.getBookById(previewToOpen.id)
                fullBook?.let {
                    homeViewModel.openBook(it)
                    liberAppViewModel.openEpub(it)
                }
            }
        }
    }

    // ── Annotation / bookmark flows ──────────────────────────────────────────
    val annotationsFlow = remember(activeBook?.id) {
        activeBook?.id?.let { homeViewModel.getAnnotationsForBook(it) } ?: emptyFlow()
    }
    val annotations by annotationsFlow.collectAsState(initial = emptyList())

    val bookmarksFlow = remember(activeBook?.id) {
        activeBook?.id?.let { homeViewModel.getBookmarksForBook(it) } ?: emptyFlow()
    }
    val bookmarks by bookmarksFlow.collectAsState(initial = emptyList())

    val pendingAnnotationRequest by homeViewModel.pendingAnnotationRequest.collectAsState()
    val playerProgress by liberAppViewModel.playerProgress.collectAsState()

    val isAudiobook = book != null && book.isAudiobook
    val isEpub = book != null && !book.isAudiobook
    val showEpubReader = isReaderOpen && isEpub

    // ── Root layout ──────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        if (showEpubReader) {
            val currentBook = book!!
            ReaderScreen(
                bookUri = currentBook.fileUri,
                bookTitle = currentBook.title,
                bookId = currentBook.id,
                bookLanguage = currentBook.language,
                dictionaryViewModel = dictionaryViewModel,
                userPreferencesRepository = userPreferencesRepository,
                initialXPointer = currentBook.lastLocator,
                annotations = annotations,
                bookmarks = bookmarks,
                pendingAnnotationRequest = pendingAnnotationRequest,
                onRequestAnnotation = { request -> homeViewModel.requestAnnotation(request) },
                onSaveLocator = { xpointer, progress ->
                    homeViewModel.saveLocator(currentBook.id, xpointer, progress)
                },
                onSaveAnnotation = { annotation -> homeViewModel.saveAnnotation(annotation) },
                onDeleteAnnotation = { annotationId -> homeViewModel.deleteAnnotation(annotationId) },
                onSaveBookmark = { bookmark -> homeViewModel.saveBookmark(bookmark) },
                onDeleteBookmark = { bookmarkId -> homeViewModel.deleteBookmark(bookmarkId) },
                onClearPendingAnnotation = { homeViewModel.clearPendingAnnotation() },
                onBack = { liberAppViewModel.closeReader() },
            )
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    Column {
                        if (isAudiobook) {
                            AnimatedVisibility(
                                visible = !isReaderOpen,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    NowPlayingBar(
                                        book = book!!,
                                        isPlaying = playWhenReadyGlobal,
                                        progress = playerProgress,
                                        onTogglePlay = { audiobookPlayerViewModel.togglePlayPause() },
                                        onRewind = { audiobookPlayerViewModel.skipBackward(15) },
                                        onForward = { audiobookPlayerViewModel.skipForward(15) },
                                        onClick = { liberAppViewModel.openReader() },
                                        modifier = Modifier
                                            .responsiveMaxWidth()
                                            .padding(horizontal = 8.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                        if (!showNavRail && isTopLevelRoute) {
                            LiberBottomNav(
                                activeTab = activeTab,
                                onTabChange = onTabChange,
                            )
                        } else if (!showNavRail && !isTopLevelRoute) {
                            Spacer(Modifier.navigationBarsPadding())
                        } else {
                            Spacer(Modifier.navigationBarsPadding())
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showNavRail && isTopLevelRoute) {
                        LiberNavRail(
                            activeTab = activeTab,
                            onTabChange = onTabChange,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 1200.dp)
                                .fillMaxSize()
                        ) {
                            AppNavHost(
                                navController = navController,
                                homeViewModel = homeViewModel,
                                collectionsViewModel = collectionsViewModel,
                                liberAppViewModel = liberAppViewModel,
                                settingsViewModel = settingsViewModel,
                                dictionaryViewModel = dictionaryViewModel,
                                onOpenBook = onOpenBook,
                                onAddBooks = onAddBooks,
                                onShareBook = { previewToShare ->
                                    scope.launch {
                                        val fullBook =
                                            homeViewModel.bookRepository.getBookById(previewToShare.id)
                                        fullBook?.let { bookToShare ->
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
                                        }
                                    }
                                },
                                onScanFolder = onScanFolder,
                                onRescanFolder = onRescanFolder,
                                onRemoveScanFolder = { source ->
                                    homeViewModel.removeScanSource(
                                        source.treeUri
                                    )
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }

        val isAudiobookOverlay = book != null && (book!!.isAudiobook)
        if (isAudiobookOverlay) {
            AnimatedVisibility(
                visible = isReaderOpen,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(500)
                ) + fadeOut(),
            ) {
                AudioPlayerScreen(
                    book = book!!,
                    liberAppViewModel = liberAppViewModel,
                    homeViewModel = homeViewModel,
                    audiobookPlayerViewModel = audiobookPlayerViewModel,
                    onBack = { liberAppViewModel.closeReader() }
                )
            }
        }
    }
}

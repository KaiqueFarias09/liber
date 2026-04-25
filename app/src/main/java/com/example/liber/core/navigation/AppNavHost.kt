package com.example.liber.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.liber.data.model.Book
import com.example.liber.data.model.ScanSource
import com.example.liber.feature.collections.CollectionDetailRoute
import com.example.liber.feature.collections.CollectionDetailViewModel
import com.example.liber.feature.collections.CollectionsViewModel
import com.example.liber.feature.dictionary.DictionaryManagementScreen
import com.example.liber.feature.dictionary.DictionaryViewModel
import com.example.liber.feature.home.HomeScreen
import com.example.liber.feature.home.HomeViewModel
import com.example.liber.feature.insights.ReadingInsightsScreen
import com.example.liber.feature.insights.ReadingInsightsViewModel
import com.example.liber.feature.library.LibraryScreen
import com.example.liber.feature.settings.ScanFoldersScreen
import com.example.liber.feature.settings.SettingsScreen
import com.example.liber.feature.settings.SettingsViewModel
import com.example.liber.ui.LiberAppViewModel

/** Route constants used by [AppNavHost] and the nav bar components. */
object AppRoute {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val READING_INSIGHTS = "reading_insights"
    const val SCAN_FOLDERS = "scan_folders"
    const val DICTIONARIES = "dictionaries"
    const val COLLECTION_DETAIL = "collection_detail/{collectionId}"

    fun collectionDetail(id: Long) = "collection_detail/$id"
}

/**
 * Declares all tab-level destinations.
 * Reader/AudioPlayer overlays are handled outside this NavHost in LiberApp.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    collectionsViewModel: CollectionsViewModel,
    liberAppViewModel: LiberAppViewModel,
    settingsViewModel: SettingsViewModel,
    dictionaryViewModel: DictionaryViewModel,
    onOpenBook: (Book) -> Unit,
    onAddBooks: () -> Unit,
    onShareBook: (Book) -> Unit,
    onScanFolder: () -> Unit,
    onRescanFolder: (ScanSource) -> Unit,
    onRemoveScanFolder: (ScanSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.HOME,
    ) {
        composable(AppRoute.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onBookClick = onOpenBook,
                liberAppViewModel = liberAppViewModel,
                modifier = modifier,
            )
        }

        composable(AppRoute.LIBRARY) {
            LibraryScreen(
                viewModel = homeViewModel,
                onBookClick = onOpenBook,
                onAddBooks = onAddBooks,
                onShareBook = onShareBook,
                collectionsViewModel = collectionsViewModel,
                liberAppViewModel = liberAppViewModel,
                onCollectionClick = { id ->
                    if (id != null) {
                        navController.navigate(AppRoute.collectionDetail(id))
                    }
                },
                modifier = modifier,
            )
        }

        composable(AppRoute.SETTINGS) {
            val scanSources by homeViewModel.scanSources.collectAsState()
            SettingsScreen(
                viewModel = settingsViewModel,
                scanSources = scanSources,
                onOpenReadingInsights = { navController.navigate(AppRoute.READING_INSIGHTS) },
                onAddBooks = onAddBooks,
                onAddScanFolder = { navController.navigate(AppRoute.SCAN_FOLDERS) },
                onRescanFolder = onRescanFolder,
                onRemoveFolder = onRemoveScanFolder,
                onOpenDictionaryManager = { navController.navigate(AppRoute.DICTIONARIES) },
                modifier = modifier,
            )
        }

        composable(AppRoute.READING_INSIGHTS) {
            val readingInsightsViewModel: ReadingInsightsViewModel = hiltViewModel()
            ReadingInsightsScreen(
                viewModel = readingInsightsViewModel,
                onBack = { navController.popBackStack() },
                modifier = modifier,
            )
        }

        composable(AppRoute.SCAN_FOLDERS) {
            val scanSources by homeViewModel.scanSources.collectAsState()
            val scanState by homeViewModel.scanState.collectAsState()

            ScanFoldersScreen(
                scanSources = scanSources,
                scanState = scanState,
                onAddFolder = onScanFolder,
                onRemoveFolder = onRemoveScanFolder,
                onRescanAll = {
                    scanSources.forEach { source -> onRescanFolder(source) }
                },
                onBack = { navController.popBackStack() },
                modifier = modifier
            )
        }

        composable(AppRoute.DICTIONARIES) {
            DictionaryManagementScreen(
                viewModel = dictionaryViewModel,
                onBack = { navController.popBackStack() },
                modifier = modifier,
            )
        }

        composable(
            route = AppRoute.COLLECTION_DETAIL,
            arguments = listOf(navArgument("collectionId") { type = NavType.LongType })
        ) {
            val detailViewModel: CollectionDetailViewModel = hiltViewModel()
            val activeBook by liberAppViewModel.activeBook.collectAsState()
            val isPlaying by liberAppViewModel.isPlaying.collectAsState()

            CollectionDetailRoute(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() },
                onOpenBook = onOpenBook,
                onShareBook = onShareBook,
                onToggleWantToRead = { book -> homeViewModel.toggleWantToRead(book.id, book.wantToRead) },
                onToggleFinished = { book -> homeViewModel.toggleFinished(book.id, book.readingProgress == 100) },
                homeViewModel = homeViewModel,
                activeBookId = activeBook?.id,
                isPlaying = isPlaying,
            )
        }
    }
}

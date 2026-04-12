package com.example.liber.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.liber.data.model.Book
import com.example.liber.data.model.ScanSource
import com.example.liber.feature.collections.CollectionsViewModel
import com.example.liber.feature.home.HomeScreen
import com.example.liber.feature.home.HomeViewModel
import com.example.liber.feature.library.LibraryScreen
import com.example.liber.feature.settings.SettingsScreen
import com.example.liber.feature.settings.SettingsViewModel
import com.example.liber.ui.LiberAppViewModel

/** Route constants used by [AppNavHost] and the nav bar components. */
object AppRoute {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
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
    onOpenBook: (Book) -> Unit,
    onAddBooks: () -> Unit,
    onShareBook: (Book) -> Unit,
    onScanFolder: () -> Unit,
    onRescanFolder: (ScanSource) -> Unit,
    onRemoveScanFolder: (ScanSource) -> Unit,
    selectedCollectionId: Long?,
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
                selectedCollectionId = selectedCollectionId,
                onCollectionClick = { id -> liberAppViewModel.setSelectedCollectionId(id) },
                modifier = modifier,
            )
        }

        composable(AppRoute.SETTINGS) {
            val scanSources by homeViewModel.scanSources.collectAsState()
            SettingsScreen(
                viewModel = settingsViewModel,
                scanSources = scanSources,
                onAddBooks = onAddBooks,
                onAddScanFolder = onScanFolder,
                onRescanFolder = onRescanFolder,
                onRemoveFolder = onRemoveScanFolder,
                modifier = modifier,
            )
        }
    }
}

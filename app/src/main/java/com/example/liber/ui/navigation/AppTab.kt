package com.example.liber.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
) {
    HOME(
        label = "Home",
        activeIcon = Icons.Filled.Home,
        inactiveIcon = Icons.Outlined.Home,
    ),
    LIBRARY(
        label = "Library",
        activeIcon = Icons.Filled.LibraryBooks,
        inactiveIcon = Icons.Outlined.LibraryBooks,
    ),
}

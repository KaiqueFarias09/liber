package com.example.liber.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Books
import com.adamglin.phosphoricons.fill.Gear
import com.adamglin.phosphoricons.fill.House
import com.adamglin.phosphoricons.regular.Books
import com.adamglin.phosphoricons.regular.Gear
import com.adamglin.phosphoricons.regular.House

enum class AppTab(
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
) {
    HOME(
        label = "Home",
        activeIcon = PhosphorIcons.Fill.House,
        inactiveIcon = PhosphorIcons.Regular.House,
    ),
    LIBRARY(
        label = "Library",
        activeIcon = PhosphorIcons.Fill.Books,
        inactiveIcon = PhosphorIcons.Regular.Books,
    ),
    SETTINGS(
        label = "Settings",
        activeIcon = PhosphorIcons.Fill.Gear,
        inactiveIcon = PhosphorIcons.Regular.Gear,
    ),
}

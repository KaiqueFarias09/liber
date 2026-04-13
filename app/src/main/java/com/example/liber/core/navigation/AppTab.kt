package com.example.liber.core.navigation

import androidx.annotation.StringRes
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
import com.example.liber.R

enum class AppTab(
    @get:StringRes val labelRes: Int,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
) {
    HOME(
        labelRes = R.string.tab_home,
        activeIcon = PhosphorIcons.Fill.House,
        inactiveIcon = PhosphorIcons.Regular.House,
    ),
    LIBRARY(
        labelRes = R.string.tab_library,
        activeIcon = PhosphorIcons.Fill.Books,
        inactiveIcon = PhosphorIcons.Regular.Books,
    ),
    SETTINGS(
        labelRes = R.string.tab_settings,
        activeIcon = PhosphorIcons.Fill.Gear,
        inactiveIcon = PhosphorIcons.Regular.Gear,
    ),
}

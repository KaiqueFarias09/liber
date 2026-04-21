package com.example.liber.core.designsystem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.liber.core.navigation.AppTab

@Composable
fun LiberBottomNav(
    activeTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSurface,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    )

    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(NavigationBarDefaults.windowInsets)
            .height(68.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        AppTab.entries.forEach { tab ->
            val selected = tab == activeTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabChange(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                        contentDescription = stringResource(tab.labelRes),
                    )
                },
                label = {
                    Text(
                        stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = itemColors,
                alwaysShowLabel = true
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview
@Composable
private fun LiberBottomNavPreview() {
    LiberTheme {
        LiberBottomNav(activeTab = AppTab.HOME, onTabChange = {})
    }
}

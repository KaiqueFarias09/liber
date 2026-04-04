package com.example.liber.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.liber.ui.navigation.AppTab
import com.example.liber.ui.theme.LiberTheme

@Composable
fun LiberBottomNav(
    activeTab: AppTab,
    onTabChange: (AppTab) -> Unit,
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color(0xFFF2F2F7),
        selectedTextColor = Color(0xFFF2F2F7),
        unselectedIconColor = Color(0xFF636366),
        unselectedTextColor = Color(0xFF636366),
        indicatorColor = Color(0xFF3A3A3C),
    )

    NavigationBar(containerColor = Color(0xFF1C1C1E)) {
        AppTab.entries.forEach { tab ->
            val selected = tab == activeTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabChange(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                        contentDescription = tab.label,
                    )
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = itemColors,
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

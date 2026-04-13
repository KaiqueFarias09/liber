package com.example.liber.core.designsystem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.liber.core.navigation.AppTab

@Composable
fun LiberNavRail(
    activeTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSurface,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxHeight(),
    ) {
        Spacer(Modifier.height(12.dp))
        AppTab.entries.forEach { tab ->
            val selected = tab == activeTab
            NavigationRailItem(
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
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview
@Composable
private fun LiberNavRailPreview() {
    LiberTheme {
        LiberNavRail(activeTab = AppTab.HOME, onTabChange = {})
    }
}

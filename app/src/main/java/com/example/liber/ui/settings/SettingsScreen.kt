package com.example.liber.ui.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowClockwise
import com.adamglin.phosphoricons.regular.FilePlus
import com.adamglin.phosphoricons.regular.FolderOpen
import com.adamglin.phosphoricons.regular.Moon
import com.adamglin.phosphoricons.regular.Palette
import com.adamglin.phosphoricons.regular.Sun
import com.adamglin.phosphoricons.regular.SunHorizon
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.data.ScanSourceEntity
import com.example.liber.data.prefs.ThemeMode
import com.example.liber.ui.components.LiberScreen

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    scanSources: List<ScanSourceEntity> = emptyList(),
    onAddBooks: () -> Unit = {},
    onAddScanFolder: () -> Unit = {},
    onRescanFolder: (ScanSourceEntity) -> Unit = {},
    onRemoveFolder: (ScanSourceEntity) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val themeMode by viewModel.themeMode.collectAsState()

    LiberScreen(
        title = "Settings",
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            SettingsSection(title = "Library") {
                LibrarySection(
                    scanSources = scanSources,
                    onAddBooks = onAddBooks,
                    onAddScanFolder = onAddScanFolder,
                    onRescanFolder = onRescanFolder,
                    onRemoveFolder = onRemoveFolder,
                )
            }

            SettingsSection(title = "Appearance") {
                ThemeSetting(
                    currentMode = themeMode,
                    onModeSelected = { viewModel.setThemeMode(it) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        content()
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun LibrarySection(
    scanSources: List<ScanSourceEntity>,
    onAddBooks: () -> Unit,
    onAddScanFolder: () -> Unit,
    onRescanFolder: (ScanSourceEntity) -> Unit,
    onRemoveFolder: (ScanSourceEntity) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Scan folders",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        if (scanSources.isEmpty()) {
            Text(
                text = "No folders added yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        } else {
            scanSources.forEach { source ->
                ScanSourceRow(
                    source = source,
                    onRescan = { onRescanFolder(source) },
                    onRemove = { onRemoveFolder(source) },
                )
            }
        }

        SettingsActionRow(
            icon = PhosphorIcons.Regular.FolderOpen,
            label = "Add scan folder…",
            onClick = onAddScanFolder,
        )

        SettingsActionRow(
            icon = PhosphorIcons.Regular.FilePlus,
            label = "Add books from files",
            onClick = onAddBooks,
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ScanSourceRow(
    source: ScanSourceEntity,
    onRescan: () -> Unit,
    onRemove: () -> Unit,
) {
    val subtitle = source.lastScannedAt?.let { ts ->
        val relative = DateUtils.getRelativeTimeSpanString(
            ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        )
        "Last scanned $relative · ${source.bookCount} book${if (source.bookCount == 1) "" else "s"}"
    } ?: "Never scanned"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = PhosphorIcons.Regular.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                Text(
                    text = source.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row {
            IconButton(onClick = onRescan) {
                Icon(
                    imageVector = PhosphorIcons.Regular.ArrowClockwise,
                    contentDescription = "Rescan",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Trash,
                    contentDescription = "Remove folder",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSetting(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = PhosphorIcons.Regular.Palette,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        val options = listOf(ThemeMode.AUTO, ThemeMode.LIGHT, ThemeMode.DARK)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEachIndexed { index, mode ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = { onModeSelected(mode) },
                    selected = currentMode == mode,
                    icon = {
                        SegmentedButtonDefaults.Icon(active = currentMode == mode) {
                            val icon = when (mode) {
                                ThemeMode.AUTO -> PhosphorIcons.Regular.SunHorizon
                                ThemeMode.LIGHT -> PhosphorIcons.Regular.Sun
                                ThemeMode.DARK -> PhosphorIcons.Regular.Moon
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        }
                    },
                ) {
                    Text(
                        text = when (mode) {
                            ThemeMode.AUTO -> "Auto"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        },
                    )
                }
            }
        }
    }
}

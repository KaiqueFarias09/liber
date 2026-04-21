package com.example.liber.feature.settings

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowClockwise
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.FilePlus
import com.adamglin.phosphoricons.regular.FolderOpen
import com.adamglin.phosphoricons.regular.Moon
import com.adamglin.phosphoricons.regular.Palette
import com.adamglin.phosphoricons.regular.Sun
import com.adamglin.phosphoricons.regular.SunHorizon
import com.adamglin.phosphoricons.regular.Translate
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.R
import com.example.liber.core.designsystem.LiberModalBottomSheet
import com.example.liber.core.designsystem.LiberScreen
import com.example.liber.core.util.UiText
import com.example.liber.data.model.ScanSource
import com.example.liber.data.repository.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    scanSources: List<ScanSource> = emptyList(),
    onAddBooks: () -> Unit = {},
    onAddScanFolder: () -> Unit = {},
    onRescanFolder: (ScanSource) -> Unit = {},
    onRemoveFolder: (ScanSource) -> Unit = {},
    onOpenDictionaryManager: () -> Unit = {},
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    LiberScreen(
        title = UiText.StringResource(R.string.tab_settings),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            SettingsSection(title = UiText.StringResource(R.string.settings_section_library)) {
                LibrarySection(
                    scanSources = scanSources,
                    onAddBooks = onAddBooks,
                    onAddScanFolder = onAddScanFolder,
                    onRescanFolder = onRescanFolder,
                    onRemoveFolder = onRemoveFolder,
                )
            }

            SettingsSection(title = UiText.StringResource(R.string.settings_section_dictionary)) {
                SettingsActionRow(
                    icon = PhosphorIcons.Regular.Translate,
                    label = UiText.StringResource(R.string.settings_dictionary_manage_action),
                    onClick = onOpenDictionaryManager,
                )
            }

            SettingsSection(title = UiText.StringResource(R.string.settings_section_appearance)) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    ThemeSetting(
                        currentMode = themeMode,
                        onModeSelected = { viewModel.setThemeMode(it) }
                    )

                    LanguageSetting(
                        currentLanguageTag = currentLanguage,
                        supportedLanguages = viewModel.supportedLanguages,
                        onLanguageSelected = { viewModel.setLanguage(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: UiText,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.asString(),
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
    scanSources: List<ScanSource>,
    onAddBooks: () -> Unit,
    onAddScanFolder: () -> Unit,
    onRescanFolder: (ScanSource) -> Unit,
    onRemoveFolder: (ScanSource) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.settings_label_scan_folders),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        if (scanSources.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_empty_scan_folders),
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
            label = UiText.StringResource(R.string.settings_action_add_scan_folder),
            onClick = onAddScanFolder,
        )

        SettingsActionRow(
            icon = PhosphorIcons.Regular.FilePlus,
            label = UiText.StringResource(R.string.settings_action_add_books_from_files),
            onClick = onAddBooks,
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: UiText,
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
                text = label.asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ScanSourceRow(
    source: ScanSource,
    onRescan: () -> Unit,
    onRemove: () -> Unit,
) {
    val subtitle = source.lastScannedAt?.let { ts ->
        val relative = DateUtils.getRelativeTimeSpanString(
            ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        ).toString()
        val bookCountLabel = if (source.bookCount == 1) {
            stringResource(R.string.label_singular_book, source.bookCount)
        } else {
            stringResource(R.string.label_plural_books, source.bookCount)
        }
        stringResource(R.string.settings_last_scanned, relative, bookCountLabel)
    } ?: stringResource(R.string.settings_never_scanned)

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
                    contentDescription = stringResource(R.string.settings_action_rescan),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Trash,
                    contentDescription = stringResource(R.string.settings_action_remove_folder),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LanguageSetting(
    currentLanguageTag: String,
    supportedLanguages: List<LanguageOptions>,
    onLanguageSelected: (String) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val currentLanguage = supportedLanguages.find { it.tag == currentLanguageTag }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
            onClick = { showSheet = true },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = MaterialTheme.shapes.small,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentLanguage?.displayName ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = PhosphorIcons.Regular.CaretDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showSheet) {
        LanguageBottomSheet(
            currentLanguageTag = currentLanguageTag,
            supportedLanguages = supportedLanguages,
            onLanguageSelected = {
                onLanguageSelected(it)
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
private fun LanguageBottomSheet(
    currentLanguageTag: String,
    supportedLanguages: List<LanguageOptions>,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    LiberModalBottomSheet(
        onDismissRequest = onDismiss,
        title = UiText.StringResource(R.string.settings_language)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            supportedLanguages.forEach { language ->
                val isSelected = language.tag == currentLanguageTag
                Surface(
                    onClick = { onLanguageSelected(language.tag) },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
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
                text = stringResource(R.string.settings_label_theme),
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
                            ThemeMode.AUTO -> stringResource(R.string.settings_theme_auto)
                            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                        },
                    )
                }
            }
        }
    }
}

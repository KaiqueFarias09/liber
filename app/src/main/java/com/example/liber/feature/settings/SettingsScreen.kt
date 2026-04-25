package com.example.liber.feature.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.ChartBar
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.FilePlus
import com.adamglin.phosphoricons.regular.FolderOpen
import com.adamglin.phosphoricons.regular.Globe
import com.adamglin.phosphoricons.regular.Info
import com.adamglin.phosphoricons.regular.Moon
import com.adamglin.phosphoricons.regular.Sun
import com.adamglin.phosphoricons.regular.SunHorizon
import com.adamglin.phosphoricons.regular.Translate
import com.example.liber.R
import com.example.liber.core.designsystem.Gambetta
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
    onOpenReadingInsights: () -> Unit = {},
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
            // APPEARANCE SECTION
            SettingsSection(title = UiText.StringResource(R.string.settings_section_appearance)) {
                ThemeSetting(
                    currentMode = themeMode,
                    onModeSelected = { viewModel.setThemeMode(it) }
                )
            }

            // GENERAL SECTION
            SettingsSection(title = UiText.StringResource(R.string.settings_section_general)) {
                SettingsRow(
                    icon = PhosphorIcons.Regular.ChartBar,
                    label = stringResource(R.string.settings_reading_insights_title),
                    onClick = onOpenReadingInsights,
                    showDivider = true,
                )
                LanguageSetting(
                    currentLanguageTag = currentLanguage,
                    supportedLanguages = viewModel.supportedLanguages,
                    onLanguageSelected = { viewModel.setLanguage(it) }
                )
            }

            // LIBRARY SECTION
            SettingsSection(title = UiText.StringResource(R.string.settings_section_library)) {
                LibrarySection(
                    scanSources = scanSources,
                    onAddBooks = onAddBooks,
                    onAddScanFolder = onAddScanFolder,
                    onOpenDictionaryManager = onOpenDictionaryManager,
                )
            }

            // ABOUT SECTION
            SettingsSection(title = UiText.StringResource(R.string.settings_section_about)) {
                SettingsRow(
                    icon = PhosphorIcons.Regular.Info,
                    label = stringResource(R.string.settings_label_about_liber),
                    value = stringResource(R.string.settings_label_version, "1.0.4"),
                    onClick = { /* No-op */ },
                    showChevron = false
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: UiText,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title.asString(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = Gambetta,
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        content()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    onClick: () -> Unit,
    showChevron: Boolean = true,
    subtitle: String? = null,
    showDivider: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Light,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (value != null) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    if (showChevron) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.CaretRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 38.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun LibrarySection(
    scanSources: List<ScanSource>,
    onAddBooks: () -> Unit,
    onAddScanFolder: () -> Unit,
    onOpenDictionaryManager: () -> Unit,
) {
    Column {
        // Scan Folders Row (Summary or individual folders)
        if (scanSources.isEmpty()) {
            SettingsRow(
                icon = PhosphorIcons.Regular.FolderOpen,
                label = stringResource(R.string.settings_label_scan_folders),
                subtitle = stringResource(R.string.settings_empty_scan_folders),
                onClick = onAddScanFolder,
                showDivider = true
            )
        } else {
            val totalBooks = scanSources.sumOf { it.bookCount }
            val lastScanSource = scanSources.maxByOrNull { it.lastScannedAt ?: 0L }
            val relativeTime = lastScanSource?.lastScannedAt?.let {
                DateUtils.getRelativeTimeSpanString(
                    it,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
            } ?: stringResource(R.string.settings_never_scanned)

            SettingsRow(
                icon = PhosphorIcons.Regular.FolderOpen,
                label = stringResource(R.string.settings_label_scan_folders),
                subtitle = stringResource(
                    R.string.settings_last_scanned,
                    relativeTime,
                    stringResource(
                        if (totalBooks == 1) R.string.label_singular_book else R.string.label_plural_books,
                        totalBooks
                    )
                ),
                value = stringResource(
                    if (scanSources.size == 1) R.string.label_singular_folder else R.string.label_plural_folders,
                    scanSources.size
                ),
                onClick = onAddScanFolder, // For now, opens the add dialog or a list
                showDivider = true
            )
        }

        SettingsRow(
            icon = PhosphorIcons.Regular.FilePlus,
            label = stringResource(R.string.settings_action_add_books_from_files),
            onClick = onAddBooks,
            showDivider = true
        )

        SettingsRow(
            icon = PhosphorIcons.Regular.Translate,
            label = stringResource(R.string.settings_dictionary_manage_action),
            onClick = onOpenDictionaryManager,
            showDivider = false
        )
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

    SettingsRow(
        icon = PhosphorIcons.Regular.Globe,
        label = stringResource(R.string.settings_language),
        value = currentLanguage?.displayName ?: "",
        onClick = { showSheet = true }
    )

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
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        val options = listOf(ThemeMode.AUTO, ThemeMode.LIGHT, ThemeMode.DARK)
        options.forEachIndexed { index, mode ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onClick = { onModeSelected(mode) },
                selected = currentMode == mode,
                label = {
                    Text(
                        text = when (mode) {
                            ThemeMode.AUTO -> stringResource(R.string.settings_theme_auto)
                            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                icon = {
                    val icon = when (mode) {
                        ThemeMode.AUTO -> PhosphorIcons.Regular.SunHorizon
                        ThemeMode.LIGHT -> PhosphorIcons.Regular.Sun
                        ThemeMode.DARK -> PhosphorIcons.Regular.Moon
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
    }
}

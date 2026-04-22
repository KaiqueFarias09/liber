package com.example.liber.feature.dictionary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Book
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.CheckCircle
import com.adamglin.phosphoricons.regular.DownloadSimple
import com.adamglin.phosphoricons.regular.FileArrowUp
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.R
import com.example.liber.core.designsystem.LiberDialog
import com.example.liber.core.designsystem.LiberScreen
import com.example.liber.core.designsystem.LiberSearchField
import com.example.liber.core.designsystem.LiberTabBar
import com.example.liber.core.designsystem.LiberTextField
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Dictionary
import com.example.liber.data.model.FreeDictCatalogItem
import java.util.Locale

private val languageMap = mapOf(
    "afr" to "Afrikaans", "deu" to "German", "eng" to "English",
    "ara" to "Arabic", "bre" to "Breton", "fra" to "French",
    "cat" to "Catalan", "fin" to "Finnish"
)

private fun getLanguageName(tag: String): String {
    return languageMap[tag] ?: try {
        Locale.forLanguageTag(tag).getDisplayLanguage(Locale.ENGLISH)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            .ifEmpty { tag.uppercase(Locale.ROOT) }
    } catch (e: Exception) {
        tag.uppercase(Locale.ROOT)
    }
}

private fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes == 0L) return "Unknown size"
    val k = 1024.0
    val sizes = arrayOf("B", "KB", "MB", "GB")
    val i = Math.floor(Math.log(bytes.toDouble()) / Math.log(k)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(k, i.toDouble()), sizes[i])
}

@Composable
fun DictionaryManagementScreen(
    viewModel: DictionaryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dictionaries by viewModel.dictionaries.collectAsState()
    val catalogState by viewModel.freeDictCatalogState.collectAsState()
    val downloadingCodes by viewModel.downloadingCodes.collectAsState()

    var activeTab by remember { mutableStateOf("installed") } // "installed" | "available"
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Dictionary?>(null) }

    LiberScreen(
        title = UiText.StringResource(R.string.settings_dictionary_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = PhosphorIcons.Regular.ArrowLeft,
                    contentDescription = stringResource(R.string.audio_control_back),
                )
            }
        },
        headerActions = {
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = PhosphorIcons.Regular.FileArrowUp,
                    contentDescription = stringResource(R.string.settings_dictionary_action_add_manual),
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val tabTitles = listOf(
                UiText.DynamicString("Installed (${dictionaries.size})"),
                UiText.DynamicString("FreeDict API")
            )
            val tabIds = listOf("installed", "available")

            LiberTabBar(
                tabs = tabTitles,
                selectedTabIndex = tabIds.indexOf(activeTab).coerceAtLeast(0),
                onTabSelected = { index -> activeTab = tabIds[index] },
                modifier = Modifier.padding(top = 8.dp)
            )

            // SEARCH BAR (only in available tab)
            if (activeTab == "available") {
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    LiberSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = UiText.DynamicString("Search languages..."),
                        onClear = { searchQuery = "" },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                if (activeTab == "installed") {
                    if (dictionaries.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No dictionaries installed yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Go to FreeDict API to download one.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        items(dictionaries, key = { it.id }) { dictionary ->
                            DictionaryCard(
                                title = dictionary.localAlias
                                    ?: (getLanguageName(dictionary.sourceLanguageTag) + " to " + (dictionary.targetLanguageTag?.let {
                                        getLanguageName(
                                            it
                                        )
                                    } ?: "Self")),
                                subtitle = dictionary.id,
                                version = dictionary.version,
                                headwords = null, // Installed dictionaries don't easily show headwords count here
                                formattedSize = formatBytes(dictionary.installSizeBytes),
                                action = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { renameTarget = dictionary },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.5f
                                                    ),
                                                    RoundedCornerShape(8.dp)
                                                )
                                        ) {
                                            Icon(
                                                imageVector = PhosphorIcons.Regular.PencilSimple,
                                                contentDescription = "Rename",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteDictionary(dictionary.id) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.5f
                                                    ),
                                                    RoundedCornerShape(8.dp)
                                                )
                                        ) {
                                            Icon(
                                                imageVector = PhosphorIcons.Regular.Trash,
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                footer = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = dictionary.isEnabled,
                                            onCheckedChange = {
                                                viewModel.setDictionaryEnabled(
                                                    dictionary.id,
                                                    it
                                                )
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .padding(end = 8.dp)
                                        )
                                        Text(
                                            text = if (dictionary.isEnabled) "Enabled" else "Disabled",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // AVAILABLE TAB
                    when (catalogState) {
                        is UiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator() }
                            }
                        }

                        is UiState.Error -> {
                            item {
                                Text(
                                    text = (catalogState as UiState.Error).message.asString(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        is UiState.Success -> {
                            val catalog =
                                (catalogState as UiState.Success<List<FreeDictCatalogItem>>).data
                            val filtered = catalog.filter {
                                it.code.contains(searchQuery, ignoreCase = true) ||
                                        getLanguageName(it.sourceLanguageTag).contains(
                                            searchQuery,
                                            ignoreCase = true
                                        ) ||
                                        getLanguageName(it.targetLanguageTag).contains(
                                            searchQuery,
                                            ignoreCase = true
                                        )
                            }
                            if (filtered.isEmpty()) {
                                item {
                                    Text(
                                        text = "No dictionaries found for \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp)
                                    )
                                }
                            } else {
                                items(filtered, key = { it.code }) { item ->
                                    val isDownloading = downloadingCodes.contains(item.code)
                                    val isInstalled =
                                        dictionaries.any { it.id == "freedict-${item.code}" }

                                    DictionaryCard(
                                        title = getLanguageName(item.sourceLanguageTag) + " to " + getLanguageName(
                                            item.targetLanguageTag
                                        ),
                                        subtitle = item.code,
                                        version = item.version,
                                        headwords = item.headwords,
                                        formattedSize = formatBytes(item.stardictSizeBytes),
                                        action = {
                                            if (isInstalled) {
                                                Icon(
                                                    imageVector = PhosphorIcons.Regular.CheckCircle,
                                                    contentDescription = "Installed",
                                                    tint = Color(0xFF4CAF50),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                OutlinedButton(
                                                    onClick = { viewModel.downloadFreeDict(item) },
                                                    enabled = !isDownloading,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    if (isDownloading) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(
                                                                14.dp
                                                            ), strokeWidth = 2.dp
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            "...",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    } else {
                                                        Icon(
                                                            PhosphorIcons.Regular.DownloadSimple,
                                                            null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            "Download",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (showCreateDialog) {
        CreateDictionaryDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, source, target, type, uri ->
                viewModel.createDictionary(name, source, target, type, uri)
                showCreateDialog = false
            },
        )
    }

    renameTarget?.let { dictionary ->
        RenameDictionaryDialog(
            dictionary = dictionary,
            onDismiss = { renameTarget = null },
            onConfirm = {
                viewModel.renameDictionary(dictionary.id, it)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun DictionaryCard(
    title: String,
    subtitle: String,
    version: String,
    headwords: Int?,
    formattedSize: String,
    action: @Composable () -> Unit,
    footer: (@Composable () -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$subtitle • v$version",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    action()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (headwords != null) {
                        Text(
                            text = String.format("%,d words", headwords),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (footer != null) {
                    footer()
                } else {
                    // Badge based on "status" (mocking stable for now if not available)
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "STABLE",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateDictionaryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, source: String, target: String?, type: String, uri: Uri?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var sourceTag by remember { mutableStateOf("en") }
    var targetTag by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("monolingual") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = DocumentFile.fromSingleUri(context, uri)?.name ?: "file"
            val extension = fileName.lowercase()
            val supportedExtensions =
                listOf(".tar.xz", ".tar.gz", ".zip", ".tar", ".dz", ".ifo", ".idx", ".dict")

            if (supportedExtensions.any { ext -> extension.endsWith(ext) }) {
                selectedUri = uri
                selectedFileName = fileName
                fileError = null
            } else {
                selectedUri = null
                selectedFileName = null
                fileError = context.getString(R.string.settings_dictionary_error_unsupported_file)
            }
        }
    }

    var typeMenuExpanded by remember { mutableStateOf(false) }

    LiberDialog(
        onDismissRequest = onDismiss,
        title = UiText.StringResource(R.string.settings_dictionary_create_title),
        dismissLabel = UiText.StringResource(R.string.action_cancel),
        confirmLabel = UiText.StringResource(R.string.action_save),
        confirmEnabled = name.trim().isNotEmpty() && sourceTag.trim()
            .isNotEmpty() && selectedUri != null,
        onConfirm = {
            onConfirm(
                name.trim(),
                sourceTag.trim(),
                targetTag.trim().ifEmpty { null },
                type,
                selectedUri
            )
        },
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // File Picker Row
            Surface(
                onClick = { launcher.launch("*/*") },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.FileArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_dictionary_field_file),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (fileError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedFileName
                                ?: stringResource(R.string.settings_dictionary_field_file_select),
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                fileError != null -> MaterialTheme.colorScheme.error
                                selectedFileName != null -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                        if (fileError != null) {
                            Text(
                                text = fileError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            LiberTextField(
                value = name,
                onValueChange = { name = it },
                label = UiText.StringResource(R.string.field_label_name),
                singleLine = true,
            )
            LiberTextField(
                value = sourceTag,
                onValueChange = { sourceTag = it },
                label = UiText.StringResource(R.string.settings_dictionary_field_source_language),
                singleLine = true,
            )
            LiberTextField(
                value = targetTag,
                onValueChange = { targetTag = it },
                label = UiText.StringResource(R.string.settings_dictionary_field_target_language),
                singleLine = true,
            )

            // Type Dropdown
            Box {
                LiberTextField(
                    value = if (type == "monolingual") stringResource(R.string.settings_dictionary_type_monolingual) else stringResource(
                        R.string.settings_dictionary_type_bilingual
                    ),
                    onValueChange = {},
                    label = UiText.StringResource(R.string.settings_dictionary_field_type),
                    readOnly = true,
                    enabled = true,
                    trailingIcon = {
                        IconButton(onClick = { typeMenuExpanded = true }) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.CaretDown,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.clickable { typeMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_dictionary_type_monolingual)) },
                        onClick = {
                            type = "monolingual"
                            typeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_dictionary_type_bilingual)) },
                        onClick = {
                            type = "bilingual"
                            typeMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDictionaryDialog(
    dictionary: Dictionary,
    onDismiss: () -> Unit,
    onConfirm: (alias: String?) -> Unit,
) {
    var alias by remember { mutableStateOf(dictionary.localAlias ?: dictionary.displayName) }

    LiberDialog(
        onDismissRequest = onDismiss,
        title = UiText.StringResource(R.string.settings_dictionary_rename_title),
        dismissLabel = UiText.StringResource(R.string.action_cancel),
        confirmLabel = UiText.StringResource(R.string.action_save),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(alias.trim().ifEmpty { null }) },
    ) {
        LiberTextField(
            value = alias,
            onValueChange = { alias = it },
            label = UiText.StringResource(R.string.field_label_name),
            singleLine = true,
        )
    }
}

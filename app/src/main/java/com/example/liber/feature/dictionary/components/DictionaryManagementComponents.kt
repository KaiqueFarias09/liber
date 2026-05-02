package com.example.liber.feature.dictionary.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.DownloadSimple
import com.adamglin.phosphoricons.regular.FileArrowUp
import com.adamglin.phosphoricons.regular.Sparkle
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.R
import com.example.liber.core.designsystem.EditorialDropdown
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.designsystem.LiberDialog
import com.example.liber.core.designsystem.LiberTextField
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Dictionary

@Composable
fun EditorialFilterSentence(
    filterFrom: String,
    onFromChange: (String) -> Unit,
    filterTo: String,
    onToChange: (String) -> Unit,
    sourceLanguages: List<String>,
    targetLanguages: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.dictionary_show_from),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Gambetta),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        EditorialDropdown(
            value = filterFrom,
            onValueChange = onFromChange,
            options = sourceLanguages
        )

        Text(
            text = stringResource(R.string.dictionary_to),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Gambetta),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        EditorialDropdown(
            value = filterTo,
            onValueChange = onToChange,
            options = targetLanguages
        )

        Text(
            text = stringResource(R.string.label_sentence_separator),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Gambetta),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// I'll skip EditorialFilterSentence for a moment and focus on what the user asked:
// "EditorialDictionaryItem, CreateDictionaryDialog, and RenameDictionaryDialog"

@Composable
fun EditorialDictionaryItem(
    title: String,
    subtitle: String,
    size: String,
    hasLemmatization: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    isInstalled: Boolean = true,
    isDownloading: Boolean = false,
    onDownload: (() -> Unit)? = null,
    wordsCount: Int? = null,
    version: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .drawBehind {
                val h = this.size.height
                val w = this.size.width
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 24.dp)
            .padding(
                top = 20.dp,
                bottom = 24.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val toText = " to "
                val parts = title.split(toText)
                if (parts.size == 2) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = parts[0],
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = Gambetta
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " " + stringResource(R.string.dictionary_to) + " ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        Text(
                            text = parts[1],
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = Gambetta
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = Gambetta
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "$subtitle ${if (version != null) "• v$version" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (isInstalled && onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Trash,
                        contentDescription = stringResource(R.string.action_delete),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            } else if (onDownload != null) {
                IconButton(
                    onClick = onDownload,
                    enabled = !isDownloading,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = PhosphorIcons.Regular.DownloadSimple,
                            contentDescription = stringResource(R.string.settings_dictionary_download_action),
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (version != null && !isInstalled) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = "v$version",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                if (wordsCount != null) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.settings_dictionary_headwords,
                            wordsCount,
                            wordsCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }

                Text(
                    text = size,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (hasLemmatization) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Sparkle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.label_smart),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (isInstalled) {
                Icon(
                    imageVector = PhosphorIcons.Regular.CaretRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun CreateDictionaryDialog(
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
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                tonalElevation = 1.dp,
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
fun RenameDictionaryDialog(
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

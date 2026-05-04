package com.example.liber.feature.reader.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Export
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.NotePencil
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Trash
import com.adamglin.phosphoricons.regular.X
import com.example.liber.R
import com.example.liber.core.designsystem.LiberTextField
import com.example.liber.core.designsystem.liberAccentContainer
import com.example.liber.core.util.InputValidator
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Annotation

/** Semi-transparent highlight color options. */
val AnnotationColorOptions = listOf(
    0x80FFD60A.toInt(), // Yellow
    0x8030D158.toInt(), // Green
    0x80FF375F.toInt(), // Pink
    0x800A84FF.toInt(), // Blue
    0x80BF5AF2.toInt(), // Purple
)

@Composable
fun AnnotationActionsSheet(
    annotation: Annotation,
    onEditNote: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val highlightColor = Color(annotation.color.toLong() and 0xFFFFFFFFL).copy(alpha = 1f)
    val hasNote = !annotation.note.isNullOrBlank()

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))

        if (!annotation.text.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(highlightColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "\"${annotation.text}\"",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = Color.Black.copy(alpha = 0.82f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        if (hasNote) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    PhosphorIcons.Regular.NotePencil, contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Text(
                    text = annotation.note!!, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        AnnotationActionRow(
            icon = PhosphorIcons.Regular.NotePencil,
            label = if (hasNote) UiText.StringResource(R.string.reader_annotation_edit_note) else UiText.StringResource(
                R.string.reader_annotation_add_note
            ),
            onClick = onEditNote
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .padding(start = 56.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )

        AnnotationActionRow(
            icon = PhosphorIcons.Regular.Export,
            label = UiText.StringResource(R.string.action_share),
            onClick = onShare
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .padding(start = 56.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )

        AnnotationActionRow(
            icon = PhosphorIcons.Regular.Trash,
            label = UiText.StringResource(R.string.reader_annotation_remove_highlight),
            tint = MaterialTheme.colorScheme.error,
            onClick = onDelete
        )

        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun AnnotationActionRow(
    icon: ImageVector,
    label: UiText,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text = label.asString(), style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
fun HighlightColorPicker(
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(50.dp))
            .liberAccentContainer(
                shape = RoundedCornerShape(50.dp),
                tintAlpha = 0.12f,
                baseColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnnotationColorOptions.forEach { colorArgb ->
                val hue = Color(colorArgb.toLong() and 0xFFFFFFFFL).copy(alpha = 1f)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(hue, CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable { onColorSelected(colorArgb) })
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    PhosphorIcons.Regular.X,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun CreateAnnotationSheet(
    annotationType: String,
    selectedText: String?,
    noteText: String,
    selectedColorArgb: Int,
    currentChapter: String?,
    onNoteTextChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val isHighlight = annotationType.lowercase() == "highlight"
    val saveLabel = if (isHighlight) {
        stringResource(R.string.reader_annotation_save_highlight)
    } else {
        stringResource(R.string.reader_annotation_save_note)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        currentChapter?.let { chapter ->
            Text(
                text = chapter, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (!selectedText.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(
                        Color(selectedColorArgb.toLong() and 0xFFFFFFFFL),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = selectedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.8f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }

        Text(
            stringResource(R.string.reader_annotation_color_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            items(AnnotationColorOptions) { colorArgb ->
                val isSelected = colorArgb == selectedColorArgb
                val color = Color(colorArgb.toLong() and 0xFFFFFFFFL)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color, CircleShape)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        )
                        .clickable { onColorChange(colorArgb) }
                )
            }
        }

        LiberTextField(
            value = noteText,
            onValueChange = { onNoteTextChange(InputValidator.validatedAnnotation(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = if (isHighlight) {
                UiText.StringResource(R.string.reader_annotation_placeholder_optional)
            } else {
                UiText.StringResource(R.string.reader_annotation_placeholder_required)
            },
            maxLines = 5,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) { Text(stringResource(R.string.action_cancel)) }
            Button(
                onClick = onSave, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text(saveLabel) }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SelectionActionsMenu(
    onHighlight: () -> Unit,
    onNote: () -> Unit,
    onDefine: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
            .liberAccentContainer(
                shape = RoundedCornerShape(8.dp),
                tintAlpha = 0.08f,
                baseColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionMenuAction(
                icon = PhosphorIcons.Regular.PencilSimple,
                label = stringResource(R.string.reader_annotation_highlight),
                onClick = onHighlight,
            )
            Box(
                Modifier
                    .height(20.dp)
                    .width(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            SelectionMenuAction(
                icon = PhosphorIcons.Regular.NotePencil,
                label = stringResource(R.string.reader_annotation_note),
                onClick = onNote,
            )
            Box(
                Modifier
                    .height(20.dp)
                    .width(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            SelectionMenuAction(
                icon = PhosphorIcons.Regular.MagnifyingGlass,
                label = stringResource(R.string.reader_action_define),
                onClick = onDefine,
            )
            Box(
                Modifier
                    .height(20.dp)
                    .width(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            SelectionMenuAction(
                icon = PhosphorIcons.Regular.Export,
                label = stringResource(R.string.action_share),
                onClick = onShare,
            )
            Box(
                Modifier
                    .height(20.dp)
                    .width(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Box(
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    PhosphorIcons.Regular.X,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SelectionMenuAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(17.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

package com.example.liber.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.PlusCircle
import com.adamglin.phosphoricons.regular.ShareNetwork
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.R
import com.example.liber.core.designsystem.BookCover
import com.example.liber.core.designsystem.CoverStyle
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book

@Composable
fun MoreOptionsSheet(
    book: Book,
    showDelete: Boolean,
    showShare: Boolean,
    onEditMetadata: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
            .padding(top = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookCover(
                    book = book,
                    style = CoverStyle.SMALL,
                    modifier = Modifier
                        .width(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Column {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        book.author?.uppercase()
                            ?: stringResource(R.string.label_unknown_author).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        MoreOptionItem(
            icon = PhosphorIcons.Regular.PencilSimple,
            title = UiText.StringResource(R.string.sheet_title_edit_metadata),
            subtitle = UiText.StringResource(R.string.sheet_subtitle_edit_metadata),
            onClick = onEditMetadata
        )
        MoreOptionItem(
            icon = PhosphorIcons.Regular.PlusCircle,
            title = UiText.StringResource(R.string.sheet_title_change_cover),
            subtitle = UiText.StringResource(R.string.sheet_subtitle_change_cover),
            onClick = onChangeCover
        )

        if (showShare || showDelete) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        if (showShare) {
            MoreOptionItem(
                icon = PhosphorIcons.Regular.ShareNetwork,
                title = UiText.StringResource(R.string.action_share_audiobook),
                subtitle = null,
                onClick = onShare
            )
        }

        if (showDelete) {
            MoreOptionItem(
                icon = PhosphorIcons.Regular.Trash,
                title = UiText.StringResource(R.string.action_remove_download),
                subtitle = null,
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun MoreOptionItem(
    icon: ImageVector,
    title: UiText,
    subtitle: UiText?,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = tint.copy(alpha = 0.8f)
        )
        Column {
            Text(
                text = title.asString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = tint
            )
            if (subtitle != null) {
                Text(
                    text = subtitle.asString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

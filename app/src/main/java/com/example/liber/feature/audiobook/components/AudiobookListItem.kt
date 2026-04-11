package com.example.liber.feature.audiobook.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.PlayCircle
import com.example.liber.core.designsystem.BookActionMenu
import com.example.liber.core.designsystem.BookCover
import com.example.liber.core.designsystem.CoverStyle
import com.example.liber.core.designsystem.DeleteBookConfirmationDialog
import com.example.liber.data.model.Book

@Composable
fun AudiobookListItem(
    book: Book,
    onClick: () -> Unit,
    onDeleteBook: () -> Unit,
    onShareBook: () -> Unit,
    onToggleWantToRead: () -> Unit,
    onToggleFinished: () -> Unit,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Cover
        Box(
            modifier = Modifier
                .size(64.dp)
                .aspectRatio(1f),
        ) {
            BookCover(
                book = book,
                style = CoverStyle.SMALL,
                isActive = isActive,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize(),
                fillBounds = true,
            )
        }

        // Title + author + status
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (!book.author.isNullOrBlank()) {
                Text(
                    text = book.author.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.5.sp,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )

                val statusText = remember(book.durationMillis, book.readingProgress) {
                    val duration = book.durationMillis ?: 0L
                    if (duration <= 0L) {
                        when {
                            book.readingProgress == 100 -> "Finished"
                            book.readingProgress > 0 -> "${book.readingProgress}%"
                            else -> "Not started"
                        }
                    } else {
                        val remainingMillis = duration * (100 - book.readingProgress) / 100
                        if (remainingMillis <= 0) {
                            "Finished"
                        } else {
                            val hours = remainingMillis / 3600000
                            val minutes = (remainingMillis % 3600000) / 60000
                            if (hours > 0) "${hours}h ${minutes}m left" else "${minutes}m left"
                        }
                    }
                }

                val statusColor = if (statusText == "Not started") {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = statusColor,
                )
            }
        }

        // More options
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.DotsThree,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            BookActionMenu(
                expanded = showMenu,
                book = book,
                onDismiss = { showMenu = false },
                onShare = onShareBook,
                onToggleWantToRead = onToggleWantToRead,
                onToggleFinished = onToggleFinished,
                onShowDetails = onShowDetails,
                onDelete = { showDeleteDialog = true },
                deleteLabel = "Delete",
            )
        }
    }

    if (showDeleteDialog) {
        DeleteBookConfirmationDialog(
            bookTitle = book.title,
            actionLabel = "Delete",
            onConfirm = {
                showDeleteDialog = false
                onDeleteBook()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

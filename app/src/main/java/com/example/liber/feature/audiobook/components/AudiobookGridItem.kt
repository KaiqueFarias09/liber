package com.example.liber.feature.audiobook.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.PlayCircle
import com.example.liber.R
import com.example.liber.core.designsystem.BookActionMenu
import com.example.liber.core.designsystem.BookCover
import com.example.liber.core.designsystem.CoverStyle
import com.example.liber.core.designsystem.DeleteBookConfirmationDialog
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book

@Composable
fun AudiobookGridItem(
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        BookCover(
            book = book,
            isActive = isActive,
            isPlaying = isPlaying,
            style = CoverStyle.LARGE,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    val duration = book.durationMillis ?: 0L
                    val remainingMillis =
                        if (duration > 0L) duration * (100 - book.readingProgress) / 100 else -1L
                    val isFinished =
                        book.readingProgress == 100 || (duration > 0L && remainingMillis <= 0)
                    val isNotStarted = !isFinished && book.readingProgress == 0 && duration <= 0L
                    val hoursLeft = if (remainingMillis > 0) remainingMillis / 3600000 else 0L
                    val minutesLeft =
                        if (remainingMillis > 0) (remainingMillis % 3600000) / 60000 else 0L

                    val remainingText = when {
                        isFinished -> stringResource(R.string.home_label_finished)
                        isNotStarted -> stringResource(R.string.label_not_started)
                        remainingMillis > 0 -> if (hoursLeft > 0)
                            stringResource(R.string.label_time_left_hours, hoursLeft, minutesLeft)
                        else
                            stringResource(R.string.label_time_left_minutes, minutesLeft)

                        else -> "${book.readingProgress}%"
                    }

                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp),
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
                    onDelete = { showMenu = false; showDeleteDialog = true },
                    deleteLabel = UiText.StringResource(R.string.action_delete),
                )
            }
        }
    }

    if (showDeleteDialog) {
        DeleteBookConfirmationDialog(
            bookTitle = book.title,
            action = UiText.StringResource(R.string.action_delete),
            onConfirm = {
                showDeleteDialog = false
                onDeleteBook()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

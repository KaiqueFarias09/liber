package com.example.liber.core.designsystem

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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.DotsThree
import com.example.liber.R
import com.example.liber.core.util.UiText
import com.example.liber.data.model.BookPreview
import com.example.liber.feature.collections.CollectionUiState
import com.example.liber.feature.collections.components.AddToCollectionDialog

@Composable
fun BookGridItem(
    book: BookPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleWantToRead: () -> Unit,
    onToggleFinished: () -> Unit,
    onShowDetails: () -> Unit,
    onDeleteBook: () -> Unit,
    onShareBook: () -> Unit,
    deleteLabel: UiText? = UiText.StringResource(R.string.action_delete_ellipsis),
    confirmDelete: Boolean = true,
    showAddToCollection: Boolean = false,
    onAddToCollection: (Long) -> Unit = {},
    collections: List<CollectionUiState> = emptyList(),
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        BookCover(
            book = book,
            style = if (book.isAudiobook) CoverStyle.LARGE else CoverStyle.SMALL,
            isActive = isActive,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isNew = book.lastOpenedAt == null && book.readingProgress == 0
            val progressText = when {
                book.readingProgress == 100 -> stringResource(R.string.label_status_finished)
                isNew -> stringResource(R.string.label_status_new)
                else -> "${book.readingProgress}%"
            }

            if (isNew) {
                Box(
                    modifier = Modifier.liberContainer(
                        shape = RoundedCornerShape(4.dp),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            } else {
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    onDelete = {
                        if (confirmDelete && deleteLabel != null) {
                            showDeleteDialog = true
                        } else {
                            onDeleteBook()
                        }
                    },
                    deleteLabel = deleteLabel,
                    showAddToCollection = showAddToCollection,
                    onAddToCollection = { showCollectionPicker = true },
                )
            }
        }
    }

    if (showCollectionPicker) {
        AddToCollectionDialog(
            collections = collections,
            onCollectionSelected = { collectionId ->
                onAddToCollection(collectionId)
                showCollectionPicker = false
            },
            onDismiss = { showCollectionPicker = false },
        )
    }

    if (showDeleteDialog && deleteLabel != null) {
        DeleteBookConfirmationDialog(
            bookTitle = book.title,
            action = deleteLabel,
            onConfirm = {
                showDeleteDialog = false
                onDeleteBook()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

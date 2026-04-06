package com.example.liber.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.DotsThree
import com.example.liber.data.Book
import com.example.liber.ui.collections.CollectionUiState

@Composable
fun BookGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onToggleWantToRead: (Book) -> Unit,
    onToggleFinished: (Book) -> Unit,
    onRenameBook: (Book, String) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onShareBook: (Book) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
    deleteLabel: String = "Remove…",
    showAddToCollection: Boolean = false,
    onAddToCollection: (Book, Long) -> Unit = { _, _ -> },
    collections: List<CollectionUiState> = emptyList(),
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth < 600.dp -> 2
            maxWidth < 1100.dp -> 5
            else -> 8
        }

        val chunkedBooks = remember(books, columns) { books.chunked(columns) }

        LazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(chunkedBooks, key = { it.first().id }) { rowBooks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    rowBooks.forEach { book ->
                        BookGridItem(
                            book = book,
                            onClick = { onBookClick(book) },
                            onToggleWantToRead = { onToggleWantToRead(book) },
                            onToggleFinished = { onToggleFinished(book) },
                            onRenameBook = { onRenameBook(book, it) },
                            onDeleteBook = { onDeleteBook(book) },
                            onShareBook = { onShareBook(book) },
                            deleteLabel = deleteLabel,
                            showAddToCollection = showAddToCollection,
                            onAddToCollection = { collectionId ->
                                onAddToCollection(
                                    book,
                                    collectionId
                                )
                            },
                            collections = collections,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill empty space if the last row is not full
                    if (rowBooks.size < columns) {
                        repeat(columns - rowBooks.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookGridItem(
    book: Book,
    onClick: () -> Unit,
    onToggleWantToRead: () -> Unit,
    onToggleFinished: () -> Unit,
    onRenameBook: (String) -> Unit,
    onDeleteBook: () -> Unit,
    onShareBook: () -> Unit,
    deleteLabel: String = "Remove…",
    showAddToCollection: Boolean = false,
    onAddToCollection: (Long) -> Unit = {},
    collections: List<CollectionUiState> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clickable(onClick = onClick),
    ) {
        BookCover(
            coverUri = book.coverUri,
            contentDescription = book.title,
            style = CoverStyle.LARGE,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress Label
            val progressText = when {
                book.readingProgress == 100 -> "FINISHED"
                book.lastOpenedAt == null && book.readingProgress == 0 -> "NEW"
                else -> "${book.readingProgress}%"
            }

            if (progressText == "NEW") {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.DotsThree,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BookActionMenu(
                    expanded = showMenu,
                    book = book,
                    onDismiss = { showMenu = false },
                    onShare = onShareBook,
                    onToggleWantToRead = onToggleWantToRead,
                    onToggleFinished = onToggleFinished,
                    onRename = { showRenameDialog = true },
                    onDelete = onDeleteBook,
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

    if (showRenameDialog) {
        RenameBookDialog(
            currentTitle = book.title,
            onConfirm = { newTitle ->
                onRenameBook(newTitle)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }
}
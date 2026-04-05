package com.example.liber.ui.library

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Bookmark as BookmarkFill
import com.adamglin.phosphoricons.regular.Bookmark
import com.adamglin.phosphoricons.regular.Books
import com.adamglin.phosphoricons.regular.CheckCircle
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.ListPlus
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.PlusCircle
import com.adamglin.phosphoricons.regular.ShareNetwork
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.data.Book
import com.example.liber.ui.components.BookCover
import com.example.liber.ui.components.CoverStyle
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.theme.LiberTheme

@Composable
fun LibraryScreen(
    books: List<Book>,
    isLoading: Boolean,
    onBookClick: (Book) -> Unit,
    onAddBooks: () -> Unit,
    onToggleWantToRead: (Book) -> Unit,
    onMarkAsFinished: (Book) -> Unit,
    onRenameBook: (Book, String) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onShareBook: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LibraryHeader(onAddBooks = onAddBooks)

        when {
            isLoading -> LoadingState()
            books.isEmpty() -> EmptyState(onAddBooks = onAddBooks)
            else -> BookGrid(
                books = books,
                onBookClick = onBookClick,
                onToggleWantToRead = onToggleWantToRead,
                onMarkAsFinished = onMarkAsFinished,
                onRenameBook = onRenameBook,
                onDeleteBook = onDeleteBook,
                onShareBook = onShareBook,
            )
        }
    }
}

// ── Convenience overload that reads directly from the ViewModel ───────────────

@Composable
fun LibraryScreen(
    viewModel: HomeViewModel,
    onBookClick: (Book) -> Unit,
    onAddBooks: () -> Unit,
    onShareBook: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    val books by viewModel.books.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LibraryScreen(
        books = books,
        isLoading = isLoading,
        onBookClick = onBookClick,
        onAddBooks = onAddBooks,
        onToggleWantToRead = { book -> viewModel.toggleWantToRead(book.id, book.wantToRead) },
        onMarkAsFinished = { book -> viewModel.markAsFinished(book.id) },
        onRenameBook = { book, newTitle -> viewModel.renameBook(book.id, newTitle) },
        onDeleteBook = { book -> viewModel.deleteBook(book.id) },
        onShareBook = onShareBook,
        modifier = modifier,
    )
}

// ── Private sub-composables ───────────────────────────────────────────────────

@Composable
private fun LibraryHeader(onAddBooks: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(onClick = onAddBooks) {
            Icon(
                imageVector = PhosphorIcons.Regular.Plus,
                contentDescription = "Add Books",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState(onAddBooks: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = PhosphorIcons.Regular.Books,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Your library is empty",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap + to add EPUB books",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAddBooks) {
                Icon(PhosphorIcons.Regular.Plus, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Books")
            }
        }
    }
}

@Composable
private fun BookGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onToggleWantToRead: (Book) -> Unit,
    onMarkAsFinished: (Book) -> Unit,
    onRenameBook: (Book, String) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onShareBook: (Book) -> Unit,
) {
    val chunkedBooks = remember(books) { books.chunked(2) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(chunkedBooks) { rowBooks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                rowBooks.forEach { book ->
                    Box(modifier = Modifier.weight(1f)) {
                        LibraryBookItem(
                            book = book,
                            onClick = { onBookClick(book) },
                            onToggleWantToRead = { onToggleWantToRead(book) },
                            onMarkAsFinished = { onMarkAsFinished(book) },
                            onRenameBook = { onRenameBook(book, it) },
                            onDeleteBook = { onDeleteBook(book) },
                            onShareBook = { onShareBook(book) },
                        )
                    }
                }
                if (rowBooks.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LibraryBookItem(
    book: Book,
    onClick: () -> Unit,
    onToggleWantToRead: () -> Unit,
    onMarkAsFinished: () -> Unit,
    onRenameBook: (String) -> Unit,
    onDeleteBook: () -> Unit,
    onShareBook: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
        
        Spacer(modifier = Modifier.height(8.dp))
        
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
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { 
                            onShareBook()
                            showMenu = false 
                        },
                        leadingIcon = { Icon(PhosphorIcons.Regular.ShareNetwork, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (book.wantToRead) "Remove from Want to Read" else "Add to Want to Read") },
                        onClick = { 
                            onToggleWantToRead()
                            showMenu = false 
                        },
                        leadingIcon = { Icon(if (book.wantToRead) PhosphorIcons.Fill.BookmarkFill else PhosphorIcons.Regular.PlusCircle, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Collection") },
                        onClick = { showMenu = false },
                        leadingIcon = { Icon(PhosphorIcons.Regular.ListPlus, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Mark as Finished") },
                        onClick = { 
                            onMarkAsFinished()
                            showMenu = false 
                        },
                        leadingIcon = { Icon(PhosphorIcons.Regular.CheckCircle, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename...") },
                        onClick = { 
                            showMenu = false 
                        },
                        leadingIcon = { Icon(PhosphorIcons.Regular.PencilSimple, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove...") },
                        onClick = { 
                            onDeleteBook()
                            showMenu = false 
                        },
                        leadingIcon = { Icon(PhosphorIcons.Regular.Trash, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewBooks = listOf(
    Book("1", "Lean UX", "Jeff Gothelf", null, Uri.EMPTY),
    Book("2", "Emotional Design", "Donald A. Norman", null, Uri.EMPTY, wantToRead = true),
    Book("3", "100 Things Every Designer Needs to Know", "Susan Weinschenk", null, Uri.EMPTY),
    Book("4", "The Design of Everyday Things", "Don Norman", null, Uri.EMPTY),
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun LibraryScreenPreview() {
    LiberTheme {
        LibraryScreen(
            books = previewBooks,
            isLoading = false,
            onBookClick = {},
            onAddBooks = {},
            onToggleWantToRead = {},
            onMarkAsFinished = {},
            onRenameBook = { _, _ -> },
            onDeleteBook = {},
            onShareBook = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun LibraryScreenEmptyPreview() {
    LiberTheme {
        LibraryScreen(
            books = emptyList(),
            isLoading = false,
            onBookClick = {},
            onAddBooks = {},
            onToggleWantToRead = {},
            onMarkAsFinished = {},
            onRenameBook = { _, _ -> },
            onDeleteBook = {},
            onShareBook = {},
        )
    }
}

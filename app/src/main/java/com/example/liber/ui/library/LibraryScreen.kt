package com.example.liber.ui.library

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.fill.Bookmark as BookmarkFill
import com.adamglin.phosphoricons.regular.Bookmark
import com.adamglin.phosphoricons.regular.Books
import com.example.liber.ui.components.BookCover
import com.example.liber.ui.components.CoverStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.example.liber.data.Book
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.theme.LiberTheme

@Composable
fun LibraryScreen(
    books: List<Book>,
    isLoading: Boolean,
    onBookClick: (Book) -> Unit,
    onAddBooks: () -> Unit,
    onToggleWantToRead: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111111)),
    ) {
        LibraryHeader(onAddBooks = onAddBooks)

        when {
            isLoading -> LoadingState()
            books.isEmpty() -> EmptyState(onAddBooks = onAddBooks)
            else -> BookGrid(
                books = books,
                onBookClick = onBookClick,
                onToggleWantToRead = onToggleWantToRead,
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
            color = Color(0xFFF2F2F7),
        )
        IconButton(onClick = onAddBooks) {
            Icon(
                imageVector = PhosphorIcons.Regular.Plus,
                contentDescription = "Add Books",
                tint = Color(0xFFF2F2F7),
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFFF2F2F7))
    }
}

@Composable
private fun EmptyState(onAddBooks: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = PhosphorIcons.Regular.Books,
                contentDescription = null,
                tint = Color(0xFF3A3A3C),
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Your library is empty",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF8E8E93),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap + to add EPUB books",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF636366),
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAddBooks,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C2C2E),
                    contentColor = Color(0xFFF2F2F7),
                ),
            ) {
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
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(books, key = { it.id }) { book ->
            LibraryBookItem(
                book = book,
                onClick = { onBookClick(book) },
                onToggleWantToRead = { onToggleWantToRead(book) },
            )
        }
    }
}

@Composable
private fun LibraryBookItem(
    book: Book,
    onClick: () -> Unit,
    onToggleWantToRead: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.67f)
                .fillMaxWidth(),
        ) {
            BookCover(
                coverUri = book.coverUri,
                contentDescription = book.title,
                style = CoverStyle.LARGE,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onToggleWantToRead,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(36.dp),
            ) {
                Icon(
                    imageVector = if (book.wantToRead) PhosphorIcons.Fill.BookmarkFill else PhosphorIcons.Regular.Bookmark,
                    contentDescription = if (book.wantToRead) "Remove from Want to Read" else "Add to Want to Read",
                    tint = if (book.wantToRead) Color(0xFF0A84FF) else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFF2F2F7),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        book.author?.let { author ->
            Text(
                text = author,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8E8E93),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
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

@Preview(showBackground = true, backgroundColor = 0xFF111111, heightDp = 800)
@Composable
private fun LibraryScreenPreview() {
    LiberTheme {
        LibraryScreen(
            books = previewBooks,
            isLoading = false,
            onBookClick = {},
            onAddBooks = {},
            onToggleWantToRead = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, heightDp = 600)
@Composable
private fun LibraryScreenEmptyPreview() {
    LiberTheme {
        LibraryScreen(
            books = emptyList(),
            isLoading = false,
            onBookClick = {},
            onAddBooks = {},
            onToggleWantToRead = {},
        )
    }
}

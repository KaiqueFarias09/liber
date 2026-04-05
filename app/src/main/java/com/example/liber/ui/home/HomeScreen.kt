package com.example.liber.ui.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.liber.R
import com.example.liber.data.Book
import com.example.liber.ui.components.BookListCard
import com.example.liber.ui.components.EmptyState
import com.example.liber.ui.components.WantToReadCover
import com.example.liber.ui.theme.LiberTheme

@Composable
fun HomeScreen(
    continueBooks: List<Book>,
    wantToReadBooks: List<Book>,
    previousBooks: List<Book>,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        item {
            Text(
                text = "Home",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            )
        }

        // ── Continue ────────────────────────────────────────────────────────
        if (continueBooks.isNotEmpty()) {
            item {
                SectionTitle(text = "Continue")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    items(continueBooks, key = { it.id }) { book ->
                        BookListCard(book = book, onClick = { onBookClick(book) }) {
                            ProgressText(book.readingProgress)
                        }
                    }
                }
            }
        }

        // ── Want to Read ────────────────────────────────────────────────────
        item { SectionDivider() }
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                SectionTitleWithChevron(text = "Want to Read")
                Text(
                    text = "Books you would like to read next.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }
        }
        item {
            if (wantToReadBooks.isEmpty()) {
                EmptyState(
                    title = "No books yet",
                    subtitle = "Tap the bookmark icon in Library to add.",
                    image = R.drawable.bookmarks_empty,
                    showImage = false,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    items(wantToReadBooks, key = { it.id }) { book ->
                        WantToReadCover(book = book, onClick = { onBookClick(book) })
                    }
                }
            }
        }

        // ── Previous ────────────────────────────────────────────────────────
        if (previousBooks.isNotEmpty()) {
            item { SectionDivider() }
            item {
                SectionTitleWithChevron(
                    text = "Previous",
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    items(previousBooks, key = { it.id }) { book ->
                        BookListCard(book = book, onClick = { onBookClick(book) }) {
                            PreviousStatusContent(book.readingProgress)
                        }
                    }
                }
            }
        }
    }
}

// ── Convenience overload that reads directly from the ViewModel ───────────────

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    val continueBooks by viewModel.continueReadingBooks.collectAsState()
    val wantToReadBooks by viewModel.wantToReadBooks.collectAsState()
    val previousBooks by viewModel.previousBooks.collectAsState()

    HomeScreen(
        continueBooks = continueBooks,
        wantToReadBooks = wantToReadBooks,
        previousBooks = previousBooks,
        onBookClick = onBookClick,
        modifier = modifier,
    )
}

// ── Private sub-composables ───────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(start = 24.dp, bottom = 12.dp),
    )
}

@Composable
private fun SectionTitleWithChevron(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Icon(
            imageVector = PhosphorIcons.Regular.CaretRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 2.dp)
                .size(20.dp),
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Composable
private fun ProgressText(progress: Int) {
    Text(
        text = "Book \u2022 $progress%",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun PreviousStatusContent(progress: Int) {
    if (progress >= 100) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Icon(
                imageVector = PhosphorIcons.Regular.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = " Finished",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        ProgressText(progress)
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewBooks = listOf(
    Book("1", "Lean UX", "Jeff Gothelf", null, Uri.EMPTY, readingProgress = 10),
    Book("2", "100 Things Every Designer Needs to Know", "Susan Weinschenk", null, Uri.EMPTY, readingProgress = 19),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenPreview() {
    LiberTheme {
        HomeScreen(
            continueBooks = previewBooks,
            wantToReadBooks = previewBooks.take(1),
            previousBooks = previewBooks,
            onBookClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun HomeScreenEmptyPreview() {
    LiberTheme {
        HomeScreen(
            continueBooks = emptyList(),
            wantToReadBooks = emptyList(),
            previousBooks = emptyList(),
            onBookClick = {},
        )
    }
}

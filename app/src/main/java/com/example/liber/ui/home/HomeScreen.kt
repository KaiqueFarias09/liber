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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.liber.data.Book
import com.example.liber.ui.components.BookListCard
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
            .background(Color(0xFF111111)),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        item {
            Text(
                text = "Home",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                color = Color(0xFFF2F2F7),
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
                    fontSize = 13.sp,
                    color = Color(0xFF636366),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }
        }
        item {
            if (wantToReadBooks.isEmpty()) {
                WantToReadEmptyState()
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
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = Color(0xFFF2F2F7),
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
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFFF2F2F7),
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF636366),
            modifier = Modifier
                .padding(start = 2.dp)
                .size(20.dp),
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = Color(0xFF2C2C2E),
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Composable
private fun ProgressText(progress: Int) {
    Text(
        text = "Book \u2022 $progress%",
        fontSize = 11.sp,
        color = Color(0xFF8E8E93),
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
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = " Finished",
                fontSize = 11.sp,
                color = Color(0xFF8E8E93),
            )
        }
    } else {
        ProgressText(progress)
    }
}

@Composable
private fun WantToReadEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No books yet.\nTap the bookmark icon in Library to add.",
            color = Color(0xFF636366),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewBooks = listOf(
    Book("1", "Lean UX", "Jeff Gothelf", null, Uri.EMPTY, readingProgress = 10),
    Book("2", "100 Things Every Designer Needs to Know", "Susan Weinschenk", null, Uri.EMPTY, readingProgress = 19),
)

@Preview(showBackground = true, backgroundColor = 0xFF111111, heightDp = 900)
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

@Preview(showBackground = true, backgroundColor = 0xFF111111, heightDp = 600)
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

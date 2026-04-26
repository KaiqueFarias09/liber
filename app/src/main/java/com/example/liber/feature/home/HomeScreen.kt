package com.example.liber.feature.home

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.CheckCircle
import com.example.liber.R
import com.example.liber.core.designsystem.BookListCard
import com.example.liber.core.designsystem.EmptyState
import com.example.liber.core.designsystem.LiberScrollableScreen
import com.example.liber.core.designsystem.LiberTheme
import com.example.liber.core.designsystem.WantToReadCover
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book

@Composable
fun HomeScreen(
    continueBooks: List<Book>,
    wantToReadBooks: List<Book>,
    previousBooks: List<Book>,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
    activeBookId: String? = null,
    isPlaying: Boolean = false,
) {
    LiberScrollableScreen(
        title = UiText.DynamicString("Liber"),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        // ── Continue ────────────────────────────────────────────────────────
        if (continueBooks.isNotEmpty()) {
            item {
                SectionTitle(text = UiText.StringResource(R.string.home_section_continue))
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    items(continueBooks, key = { it.id }) { book ->
                        BookListCard(
                            book = book,
                            onClick = { onBookClick(book) },
                            isActive = book.id == activeBookId,
                            isPlaying = isPlaying,
                        ) {
                            ProgressText(book)
                        }
                    }
                }
            }
        }

        // ── Want to Read ────────────────────────────────────────────────────
        item { SectionDivider() }
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                SectionTitleWithChevron(text = UiText.StringResource(R.string.home_section_want_to_read))
                Text(
                    text = stringResource(R.string.home_subtitle_want_to_read),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }
        }
        item {
            if (wantToReadBooks.isEmpty()) {
                EmptyState(
                    title = UiText.StringResource(R.string.home_empty_title_want_to_read),
                    subtitle = UiText.StringResource(R.string.home_empty_subtitle_want_to_read),
                    image = R.drawable.want_to_read_empty,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalAlignment = Alignment.Bottom
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
                    text = UiText.StringResource(R.string.home_section_previous),
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
                        BookListCard(
                            book = book,
                            onClick = { onBookClick(book) },
                            isActive = book.id == activeBookId,
                            isPlaying = isPlaying,
                        ) {
                            PreviousStatusContent(book)
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
    liberAppViewModel: com.example.liber.ui.LiberAppViewModel,
    modifier: Modifier = Modifier,
) {
    val continueBooks by viewModel.continueReadingBooks.collectAsState()
    val wantToReadBooks by viewModel.wantToReadBooks.collectAsState()
    val previousBooks by viewModel.previousBooks.collectAsState()
    val activeBook by liberAppViewModel.activeBook.collectAsState()
    val isPlaying by liberAppViewModel.isPlaying.collectAsState()

    HomeScreen(
        continueBooks = continueBooks,
        wantToReadBooks = wantToReadBooks,
        previousBooks = previousBooks,
        onBookClick = onBookClick,
        activeBookId = activeBook?.id,
        isPlaying = isPlaying,
        modifier = modifier,
    )
}

// ── Private sub-composables ───────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: UiText, modifier: Modifier = Modifier) {
    Text(
        text = text.asString(),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(start = 24.dp, bottom = 12.dp),
    )
}

@Composable
private fun SectionTitleWithChevron(text: UiText, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = text.asString(),
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
private fun ProgressText(book: Book) {
    val typeLabel = if (book.isAudiobook) stringResource(R.string.home_label_audiobook)
    else stringResource(R.string.home_label_book)
    Text(
        text = stringResource(R.string.home_label_progress, typeLabel, book.readingProgress),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun PreviousStatusContent(book: Book) {
    if (book.readingProgress >= 100) {
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
            val typeLabel = if (book.isAudiobook) stringResource(R.string.home_label_audiobook)
            else stringResource(R.string.home_label_book)
            Text(
                text = " " + stringResource(R.string.home_label_finished_status, typeLabel),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        ProgressText(book)
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewBooks = listOf(
    Book("1", "Lean UX", "Jeff Gothelf", null, Uri.EMPTY, readingProgress = 10),
    Book(
        "2",
        "100 Things Every Designer Needs to Know",
        "Susan Weinschenk",
        null,
        Uri.EMPTY,
        readingProgress = 19
    ),
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

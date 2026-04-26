package com.example.liber.core.designsystem

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.liber.data.model.Book

/**
 * Horizontal book card used in "Continue" and "Previous" rows.
 *
 * The [statusContent] slot is placed below the author line, letting each
 * call site supply its own progress/status indicator.
 */
@Composable
fun BookListCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    statusContent: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .width(320.dp) // Slightly wider for audiobook vinyl
            .height(112.dp)
            .liberContainer()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(
                book = book,
                style = CoverStyle.SMALL,
                isActive = isActive,
                isPlaying = isPlaying,
                modifier = Modifier.size(if (book.isAudiobook) 80.dp else 56.dp, 80.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                book.author?.let { author ->
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                statusContent()
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun BookListCardPreview() {
    LiberTheme {
        BookListCard(
            book = Book(
                id = "1",
                title = "Lean UX: Designing Great Products with Agile Teams",
                author = "Jeff Gothelf",
                coverUri = null,
                fileUri = Uri.EMPTY,
                readingProgress = 42,
            ),
            onClick = {},
        ) {
            Text(
                text = "Book \u2022 42%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

package com.example.liber.ui.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.liber.data.Book
import com.example.liber.ui.theme.LiberTheme

/** Large book cover shown in the "Want to Read" horizontal row. */
@Composable
fun WantToReadCover(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BookCover(
        book = book,
        style = CoverStyle.LARGE,
        isActive = false,
        isPlaying = false,
        modifier = modifier
            .width(if (book.isAudiobook) 200.dp else 144.dp)
            .height(210.dp)
            .clickable(onClick = onClick)
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun WantToReadCoverPreview() {
    LiberTheme {
        WantToReadCover(
            book = Book(
                id = "1",
                title = "Sample Book",
                author = "Author",
                coverUri = null,
                fileUri = Uri.EMPTY
            ),
            onClick = {},
        )
    }
}

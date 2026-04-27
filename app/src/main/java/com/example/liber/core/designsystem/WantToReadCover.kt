package com.example.liber.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.liber.data.model.BookPreview

/** Large book cover shown in the \"Want to Read\" horizontal row. */
@Composable
fun WantToReadCover(
    book: BookPreview,
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
            .clickable(onClick = onClick)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun WantToReadCoverPreview() {
    LiberTheme {
        WantToReadCover(
            book = BookPreview(
                id = "1",
                title = "Sample Book",
                author = "Author",
                coverUri = null,
                mediaType = "epub"
            ),
            onClick = {},
        )
    }
}

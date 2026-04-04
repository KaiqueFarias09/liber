package com.example.liber.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.liber.data.Book
import com.example.liber.ui.theme.LiberTheme

/** Large book cover shown in the "Want to Read" horizontal row. */
@Composable
fun WantToReadCover(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = book.coverUri,
        contentDescription = book.title,
        modifier = modifier
            .width(144.dp)
            .height(210.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF3A3A3C))
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop,
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
                fileUri = Uri.EMPTY,
            ),
            onClick = {},
        )
    }
}

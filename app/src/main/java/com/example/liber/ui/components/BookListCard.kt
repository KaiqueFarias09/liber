package com.example.liber.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.liber.data.Book
import com.example.liber.ui.theme.LiberTheme

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
    statusContent: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .width(300.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = book.coverUri,
                contentDescription = book.title,
                modifier = Modifier
                    .size(width = 56.dp, height = 80.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF3A3A3C)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF2F2F7),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                book.author?.let { author ->
                    Text(
                        text = author,
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93),
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

@Preview(showBackground = true, backgroundColor = 0xFF111111)
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
                fontSize = 11.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

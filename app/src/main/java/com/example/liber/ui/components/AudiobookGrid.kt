package com.example.liber.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.liber.data.Book

@Composable
fun AudiobookGrid(
    audiobooks: List<Book>,
    onBookClick: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    activeBookId: String? = null,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
    ) {
        items(audiobooks, key = { it.id }) { book ->
            AudiobookGridItem(
                book = book,
                onClick = { onBookClick(book) },
                isActive = book.id == activeBookId,
                isPlaying = isPlaying
            )
        }
    }
}

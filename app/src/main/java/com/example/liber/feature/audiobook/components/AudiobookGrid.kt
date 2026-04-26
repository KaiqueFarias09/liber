package com.example.liber.feature.audiobook.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.Rows
import com.adamglin.phosphoricons.regular.SquaresFour
import com.example.liber.R
import com.example.liber.core.designsystem.LiberContextMenuItem
import com.example.liber.core.designsystem.LiberDropdownMenu
import com.example.liber.data.model.Book
import com.example.liber.feature.library.LibrarySortOption
import com.example.liber.feature.library.LibraryViewMode

@Composable
fun AudiobookGrid(
    audiobooks: List<Book>,
    onBookClick: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onShareBook: (Book) -> Unit,
    onToggleWantToRead: (Book) -> Unit,
    onToggleFinished: (Book) -> Unit,
    onShowDetails: (Book) -> Unit,
    modifier: Modifier = Modifier,
    activeBookId: String? = null,
    isPlaying: Boolean = false,
    viewMode: LibraryViewMode = LibraryViewMode.GRID,
    onViewModeChange: (LibraryViewMode) -> Unit = {},
    sortOption: LibrarySortOption = LibrarySortOption.RECENT,
    onSortOptionChange: (LibrarySortOption) -> Unit = {},
) {
    val sortedAudiobooks = remember(audiobooks, sortOption) {
        when (sortOption) {
            LibrarySortOption.TITLE -> audiobooks.sortedBy { it.title.lowercase() }
            LibrarySortOption.AUTHOR -> audiobooks.sortedBy { it.author?.lowercase() ?: "" }
            LibrarySortOption.RECENT -> audiobooks.sortedWith(
                compareByDescending<Book> { it.lastOpenedAt != null }
                    .thenByDescending { it.lastOpenedAt }
            )
        }
    }

    val contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "viewMode",
            modifier = Modifier.fillMaxSize(),
        ) { mode ->
            if (mode == LibraryViewMode.GRID) {
                LazyVerticalGrid(
                    columns = when {
                        maxWidth < 400.dp -> GridCells.Fixed(2)
                        maxWidth < 600.dp -> GridCells.Fixed(3)
                        else -> GridCells.Adaptive(160.dp)
                    },
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AudiobooksToolbar(
                            audiobookCount = audiobooks.size,
                            sortOption = sortOption,
                            onSortChange = onSortOptionChange,
                            viewMode = viewMode,
                            onViewModeChange = onViewModeChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        )
                    }
                    items(sortedAudiobooks, key = { it.id }) { book ->
                        AudiobookGridItem(
                            book = book,
                            onClick = { onBookClick(book) },
                            onDeleteBook = { onDeleteBook(book) },
                            onShareBook = { onShareBook(book) },
                            onToggleWantToRead = { onToggleWantToRead(book) },
                            onToggleFinished = { onToggleFinished(book) },
                            onShowDetails = { onShowDetails(book) },
                            isActive = book.id == activeBookId,
                            isPlaying = isPlaying
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        AudiobooksToolbar(
                            audiobookCount = audiobooks.size,
                            sortOption = sortOption,
                            onSortChange = onSortOptionChange,
                            viewMode = viewMode,
                            onViewModeChange = onViewModeChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        )
                    }
                    items(sortedAudiobooks, key = { it.id }) { book ->
                        AudiobookListItem(
                            book = book,
                            onClick = { onBookClick(book) },
                            onDeleteBook = { onDeleteBook(book) },
                            onShareBook = { onShareBook(book) },
                            onToggleWantToRead = { onToggleWantToRead(book) },
                            onToggleFinished = { onToggleFinished(book) },
                            onShowDetails = { onShowDetails(book) },
                            isActive = book.id == activeBookId,
                            isPlaying = isPlaying,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudiobooksToolbar(
    audiobookCount: Int,
    sortOption: LibrarySortOption,
    onSortChange: (LibrarySortOption) -> Unit,
    viewMode: LibraryViewMode,
    onViewModeChange: (LibraryViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (audiobookCount == 1) stringResource(
                R.string.label_singular_audiobook,
                audiobookCount
            )
            else stringResource(R.string.label_plural_audiobooks, audiobookCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Sort dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { sortMenuExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.sort_label) + " ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = sortOption.label.asString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Icon(
                        imageVector = PhosphorIcons.Regular.CaretDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(12.dp)
                            .rotate(if (sortMenuExpanded) 180f else 0f),
                    )
                }

                LiberDropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                ) {
                    LibrarySortOption.entries.forEach { option ->
                        val isActive = sortOption == option
                        LiberContextMenuItem(
                            label = option.label,
                            icon = if (isActive) PhosphorIcons.Regular.Check else null,
                            onClick = {
                                onSortChange(option)
                                sortMenuExpanded = false
                            },
                        )
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            )

            // View mode toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                tonalElevation = 1.dp,
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    ViewToggleButton(
                        selected = viewMode == LibraryViewMode.GRID,
                        onClick = { onViewModeChange(LibraryViewMode.GRID) },
                        icon = {
                            Icon(
                                imageVector = PhosphorIcons.Regular.SquaresFour,
                                contentDescription = "Grid view",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                    ViewToggleButton(
                        selected = viewMode == LibraryViewMode.LIST,
                        onClick = { onViewModeChange(LibraryViewMode.LIST) },
                        icon = {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Rows,
                                contentDescription = "List view",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val iconTint = if (selected) MaterialTheme.colorScheme.onBackground
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides iconTint) {
                icon()
            }
        }
    }
}

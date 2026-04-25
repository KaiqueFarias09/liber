package com.example.liber.feature.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.Plus
import com.example.liber.R
import com.example.liber.core.designsystem.BookCover
import com.example.liber.core.designsystem.CoverStyle
import com.example.liber.core.designsystem.EmptyState
import com.example.liber.core.designsystem.LiberFAB
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book
import com.example.liber.feature.collections.components.CollectionNameDialog

// ── List screen ───────────────────────────────────────────────────────────────

@Composable
fun CollectionsListScreen(
    collections: List<CollectionUiState>,
    onCollectionClick: (CollectionUiState) -> Unit,
    onCreateCollection: (String) -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (collections.isNotEmpty()) {
                LiberFAB(
                    onClick = { showCreateDialog = true },
                    icon = PhosphorIcons.Regular.Plus,
                    contentDescription = stringResource(R.string.action_new_collection),
                )
            }
        },
    ) { innerPadding ->
        if (collections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = UiText.StringResource(R.string.empty_collections_title),
                    subtitle = UiText.StringResource(R.string.empty_collections_subtitle),
                    image = R.drawable.collections_empty,
                    actionLabel = UiText.StringResource(R.string.empty_collections_action),
                    onAction = { showCreateDialog = true },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    top = 8.dp, // Match BookGrid's top padding
                    end = 24.dp,
                    bottom = 80.dp + innerPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(collections, key = { it.id }) { collection ->
                    CollectionShelfRow(
                        collection = collection,
                        onClick = { onCollectionClick(collection) },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CollectionNameDialog(
            title = UiText.StringResource(R.string.dialog_title_new_collection),
            initialName = "",
            onConfirm = { name ->
                onCreateCollection(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }
}


// ── Collection shelf card ─────────────────────────────────────────────────────

@Composable
private fun CollectionShelfRow(
    collection: CollectionUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(
                top = 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 12.dp
            )
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (collection.books.size == 1) stringResource(
                            R.string.label_singular_book,
                            collection.books.size
                        )
                        else stringResource(R.string.label_plural_books, collection.books.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.CaretRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stacked covers
            StackedBookCovers(
                books = collection.books,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Stacked covers ────────────────────────────────────────────────────────────

@Composable
private fun StackedBookCovers(
    books: List<Book>,
    modifier: Modifier = Modifier,
) {
    val displayBooks = books.take(8)
    val extraCount = (books.size - 8).coerceAtLeast(0)

    if (displayBooks.isEmpty()) {
        Box(
            modifier = modifier
                .height(40.dp) // Maintain some height for the card layout
                .fillMaxWidth(),
        )
        return
    }

    val coverWidth = 72.dp
    val step = 42.dp

    // contentAlignment = BottomStart so every cover's bottom edge sits at the shelf
    // floor. offset(x) shifts each cover rightward graphically. clipToBounds trims
    // any cover that is taller than shelfHeight at the top.
    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.BottomStart,
    ) {
        displayBooks.forEachIndexed { index, book ->
            BookCover(
                book = book,
                style = CoverStyle.SMALL,
                isActive = false,
                isPlaying = false,
                modifier = Modifier
                    .offset(x = step * index)
                    .width(coverWidth),
            )
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .offset(x = step * displayBooks.size)
                    .width(coverWidth)
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$extraCount",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

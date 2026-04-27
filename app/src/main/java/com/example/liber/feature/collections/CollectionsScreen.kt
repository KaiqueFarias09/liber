package com.example.liber.feature.collections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.pluralStringResource
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
import com.example.liber.data.model.BookPreview
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
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = when {
                    maxWidth < 600.dp -> 1
                    else -> 2
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        top = 8.dp, // Match BookGrid's top padding
                        end = 24.dp,
                        bottom = 80.dp + innerPadding.calculateBottomPadding()
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(collections, key = { "collection_${it.id}" }) { collection ->
                        CollectionShelfRow(
                            collection = collection,
                            onClick = { onCollectionClick(collection) },
                        )
                    }
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
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
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
                        text = pluralStringResource(
                            R.plurals.label_books,
                            collection.totalBooks,
                            collection.totalBooks
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp),
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
                previews = collection.previews,
                totalBooks = collection.totalBooks,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Stacked covers ────────────────────────────────────────────────────────────

@Composable
private fun StackedBookCovers(
    previews: List<BookPreview>,
    totalBooks: Int,
    modifier: Modifier = Modifier,
) {
    val displayBooks = previews.take(8)
    val extraCount = (totalBooks - 8).coerceAtLeast(0)
    val coverWidth = 72.dp
    val step = 42.dp
    val shelfHeight = 108.dp // Height of a 2:3 book with 72dp width

    // contentAlignment = BottomStart so every cover's bottom edge sits at the shelf
    // floor. offset(x) shifts each cover rightward graphically. clipToBounds trims
    // any cover that is taller than shelfHeight at the top.
    Box(
        modifier = modifier
            .height(shelfHeight)
            .fillMaxWidth()
            .clipToBounds(),
        contentAlignment = Alignment.BottomStart,
    ) {
        displayBooks.forEachIndexed { index, preview ->
            BookCover(
                book = preview,
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
                    .height(shelfHeight)
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

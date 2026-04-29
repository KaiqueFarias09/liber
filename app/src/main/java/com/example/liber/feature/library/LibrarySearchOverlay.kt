package com.example.liber.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Book
import com.adamglin.phosphoricons.regular.BookOpen
import com.adamglin.phosphoricons.regular.Headphones
import com.example.liber.R
import com.example.liber.core.designsystem.BookCover
import com.example.liber.core.designsystem.EditorialSearchField
import com.example.liber.core.designsystem.EmptyState
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.designsystem.MaxContentWidth
import com.example.liber.core.designsystem.liberOutlinedContainer
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.local.DictionaryEntryWithSenses
import com.example.liber.data.model.BookPreview

@Composable
fun LibrarySearchOverlay(
    isOpen: Boolean,
    onClose: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    booksState: UiState<List<BookPreview>>,
    onBookClick: (BookPreview) -> Unit,
    recentSearches: List<String> = emptyList(),
    onSearch: (String) -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onOpenDictionaryManager: () -> Unit = {},
    hasDictionaries: Boolean = false,
    searchType: SearchType = SearchType.ALL,
    onSearchTypeChange: (SearchType) -> Unit = {},
    dictionaryResults: UiState<List<DictionaryEntryWithSenses>> = UiState.Success(emptyList()),
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= MaxContentWidth
        val alpha by animateFloatAsState(
            targetValue = if (isOpen) 1f else 0f,
            animationSpec = tween(400),
            label = "search_alpha"
        )

        if (isOpen || (alpha > 0f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f * alpha))
                    .clickable(enabled = isOpen && isTablet) { onClose() },
                contentAlignment = if (isTablet) Alignment.TopCenter else Alignment.TopStart
            ) {
                AnimatedVisibility(
                    visible = isOpen,
                    enter = if (isTablet) scaleIn(initialScale = 0.95f) + fadeIn() else slideInVertically(
                        initialOffsetY = { it }),
                    exit = if (isTablet) scaleOut(targetScale = 0.95f) + fadeOut() else fadeOut() + slideOutVertically { it }
                ) {
                    val containerModifier = if (isTablet) {
                        Modifier
                            .padding(top = 96.dp)
                            .width(MaxContentWidth)
                            .fillMaxHeight(0.7f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(enabled = false) {}
                            .liberOutlinedContainer(shape = RoundedCornerShape(24.dp))
                    } else {
                        Modifier.fillMaxSize()
                    }

                    Surface(
                        modifier = containerModifier,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Back Button
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .padding(start = 16.dp, top = if (isTablet) 16.dp else 8.dp)
                                    .size(40.dp)
                                    .background(
                                        Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.ArrowLeft,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Search Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 24.dp,
                                        bottom = 16.dp,
                                        start = 24.dp,
                                        end = 24.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EditorialSearchField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    placeholder = if (isTablet) "Search library, dictionary, or store..." else "Search...",
                                    onClear = { onSearchQueryChange("") },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            onSearch(searchQuery)
                                        }
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }),
                                    modifier = Modifier
                                        .weight(1f)
                                )
                            }

                            // Search Content
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                if (searchQuery.isEmpty()) {
                                    if (recentSearches.isNotEmpty()) {
                                        Text(
                                            text = "RECENT SEARCHES",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.5.sp,
                                                fontSize = 10.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.4f
                                            )
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        recentSearches.forEach { term ->
                                            Text(
                                                text = term,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontFamily = Gambetta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onSearchQueryChange(term) }
                                                    .padding(vertical = 8.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(32.dp))
                                    }

                                    Text(
                                        text = "QUICK FILTERS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            fontSize = 10.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.4f
                                        )
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        QuickFilterChip(
                                            icon = PhosphorIcons.Regular.BookOpen,
                                            label = "E-books",
                                            selected = searchType == SearchType.BOOKS,
                                            onClick = {
                                                val nextType =
                                                    if (searchType == SearchType.BOOKS) SearchType.ALL else SearchType.BOOKS
                                                onSearchTypeChange(nextType)
                                            }
                                        )
                                        QuickFilterChip(
                                            icon = PhosphorIcons.Regular.Headphones,
                                            label = "Audiobooks",
                                            selected = searchType == SearchType.AUDIOBOOKS,
                                            onClick = {
                                                val nextType =
                                                    if (searchType == SearchType.AUDIOBOOKS) SearchType.ALL else SearchType.AUDIOBOOKS
                                                onSearchTypeChange(nextType)
                                            }
                                        )
                                        QuickFilterChip(
                                            icon = PhosphorIcons.Regular.Book,
                                            label = "Dictionary",
                                            selected = searchType == SearchType.DICTIONARY,
                                            onClick = {
                                                val nextType =
                                                    if (searchType == SearchType.DICTIONARY) SearchType.ALL else SearchType.DICTIONARY
                                                onSearchTypeChange(nextType)
                                            }
                                        )
                                    }
                                } else {
                                    val isDictFilter = searchType == SearchType.DICTIONARY
                                    val isEpubFilter = searchType == SearchType.BOOKS
                                    val isAudioFilter = searchType == SearchType.AUDIOBOOKS

                                    Text(
                                        text = if (isDictFilter) "IN DICTIONARIES" else "IN YOUR LIBRARY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            fontSize = 10.sp
                                        ),
                                        color = Color(0xFFD86A77).copy(alpha = 0.6f)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider(color = Color(0xFFD86A77).copy(alpha = 0.1f))

                                    when {
                                        searchType == SearchType.DICTIONARY -> {
                                            when (dictionaryResults) {
                                                is UiState.Success -> {
                                                    val results = dictionaryResults.data
                                                    if (results.isEmpty()) {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (!hasDictionaries) {
                                                                EmptyState(
                                                                    title = UiText.DynamicString("No dictionaries found"),
                                                                    subtitle = UiText.DynamicString(
                                                                        "You haven't downloaded any dictionaries yet. Download one to start looking up words."
                                                                    ),
                                                                    image = R.drawable.library_empty,
                                                                    actionLabel = UiText.DynamicString(
                                                                        "Manage Dictionaries"
                                                                    ),
                                                                    onAction = onOpenDictionaryManager,
                                                                    modifier = Modifier.padding(
                                                                        bottom = 48.dp
                                                                    )
                                                                )
                                                            } else {
                                                                EmptyState(
                                                                    title = UiText.DynamicString("No results in dictionary"),
                                                                    subtitle = UiText.DynamicString(
                                                                        "Try searching for a different word."
                                                                    ),
                                                                    image = R.drawable.library_empty,
                                                                    modifier = Modifier.padding(
                                                                        bottom = 48.dp
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        LazyColumn(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            contentPadding = PaddingValues(vertical = 12.dp),
                                                            verticalArrangement = Arrangement.spacedBy(
                                                                16.dp
                                                            )
                                                        ) {
                                                            items(results) { entry ->
                                                                DictionarySearchResultItem(
                                                                    entry = entry,
                                                                    onClick = { /* Handle dictionary entry click if needed */ }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                else -> {}
                                            }
                                        }

                                        else -> {
                                            when (booksState) {
                                                is UiState.Success -> {
                                                    val results = booksState.data
                                                    if (results.isEmpty()) {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            when {
                                                                isEpubFilter -> {
                                                                    EmptyState(
                                                                        title = UiText.DynamicString(
                                                                            "No E-books found"
                                                                        ),
                                                                        subtitle = UiText.DynamicString(
                                                                            "Try a different search term or add more books."
                                                                        ),
                                                                        image = R.drawable.library_empty,
                                                                        modifier = Modifier.padding(
                                                                            bottom = 48.dp
                                                                        )
                                                                    )
                                                                }

                                                                isAudioFilter -> {
                                                                    EmptyState(
                                                                        title = UiText.DynamicString(
                                                                            "No Audiobooks found"
                                                                        ),
                                                                        subtitle = UiText.DynamicString(
                                                                            "Try a different search term or add more audiobooks."
                                                                        ),
                                                                        image = R.drawable.audiobooks_empty,
                                                                        modifier = Modifier.padding(
                                                                            bottom = 48.dp
                                                                        )
                                                                    )
                                                                }

                                                                else -> {
                                                                    EmptyState(
                                                                        title = UiText.StringResource(
                                                                            R.string.error_no_results
                                                                        ),
                                                                        subtitle = UiText.StringResource(
                                                                            R.string.reader_search_no_results_subtitle
                                                                        ),
                                                                        image = R.drawable.library_empty,
                                                                        modifier = Modifier.padding(
                                                                            bottom = 48.dp
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        LazyColumn(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            contentPadding = PaddingValues(vertical = 12.dp),
                                                            verticalArrangement = Arrangement.spacedBy(
                                                                16.dp
                                                            )
                                                        ) {
                                                            items(results) { book ->
                                                                SearchResultItem(
                                                                    book = book,
                                                                    onClick = { onBookClick(book) }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                else -> {}
                                            }
                                        }
                                    }
                                }
                            }

                            if (isTablet) {
                                // Search Footer
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.2f
                                            )
                                        )
                                        .padding(horizontal = 24.dp, vertical = 12.dp)
                                        .drawBehind {
                                            drawLine(
                                                color = Color.LightGray.copy(alpha = 0.2f),
                                                start = Offset.Zero,
                                                end = Offset(size.width, 0f),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "UNIVERSAL SEARCH MODE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            fontSize = 9.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.3f
                                        )
                                    )
                                    Text(
                                        text = "Press ESC to close",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.4f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickFilterChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = 0.5f
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DictionarySearchResultItem(
    entry: DictionaryEntryWithSenses,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = entry.entry.headword,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Gambetta,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (entry.senses.isNotEmpty()) {
            Text(
                text = entry.senses.first().definition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    book: BookPreview,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini Cover
        BookCover(
            book = book,
            modifier = Modifier
                .width(40.dp)
                .height(56.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Gambetta,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${book.author ?: "Unknown Author"} • ${if (book.isAudiobook) "Audiobook" else "E-book"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

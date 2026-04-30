package com.example.liber.feature.dictionary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Book
import com.adamglin.phosphoricons.regular.Sparkle
import com.adamglin.phosphoricons.regular.X
import com.example.liber.core.designsystem.AppErrorState
import com.example.liber.core.designsystem.EditorialSearchField
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.designsystem.LiberCollapsingScreen
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.local.DictionaryEntryWithSenses
import com.example.liber.data.model.Dictionary
import com.example.liber.feature.dictionary.components.DictionaryEntryItem

@Composable
fun DictionaryViewerScreen(
    dictionary: Dictionary,
    viewModel: DictionaryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val browseState by viewModel.browseState.collectAsState()
    val browseQuery by viewModel.browseQuery.collectAsState()
    val isBrowsingMore by viewModel.isBrowsingMore.collectAsState()
    val lemmatizationStatus by viewModel.lemmatizationStatus.collectAsState()
    val languagesWithLemmas by viewModel.languagesWithLemmas.collectAsState()
    val smartInfoDismissed by viewModel.smartRecognitionInfoDismissed.collectAsState()

    val normalizedLang = viewModel.normalizeLanguageTag(dictionary.sourceLanguageTag)
    val hasLemmatization =
        languagesWithLemmas.contains(normalizedLang) || lemmatizationStatus.containsKey(
            normalizedLang
        )
    val showSmartInfo = hasLemmatization && !smartInfoDismissed

    val listState = rememberLazyListState()

    // Infinite scroll trigger
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex =
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            // Load more when we are 5 items from the bottom
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 5
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMoreEntries()
        }
    }

    // Reset scroll position when search query changes
    LaunchedEffect(browseQuery) {
        listState.scrollToItem(0)
    }

    val displayTitle = dictionary.localAlias
        ?: (getLanguageName(dictionary.sourceLanguageTag) + " to " + (dictionary.targetLanguageTag?.let {
            getLanguageName(it)
        } ?: "Self"))

    LiberCollapsingScreen(
        title = UiText.DynamicString(displayTitle),
        onBack = onBack,
        modifier = modifier
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            if (showSmartInfo) {
                // Info block for lemmatization
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        tonalElevation = 1.dp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Sparkle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Smart Word Recognition",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "This dictionary automatically finds root words (Lemmatization). For example, it smartly finds definitions for inflected forms.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissSmartRecognitionInfo() },
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.X,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f))
                        .padding(vertical = 8.dp)
                ) {
                    EditorialSearchField(
                        value = browseQuery,
                        onValueChange = { viewModel.browseDictionary(dictionary.id, it) },
                        placeholder = "Search words...",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            when (browseState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        val errorState = browseState as UiState.Error
                        AppErrorState(
                            title = errorState.title,
                            message = errorState.message,
                            onRetry = { viewModel.browseDictionary(dictionary.id, browseQuery) },
                            fillMaxSize = false,
                        )
                    }
                }

                is UiState.Success -> {
                    val entries =
                        (browseState as UiState.Success<List<DictionaryEntryWithSenses>>).data
                    if (entries.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No words found for \"$browseQuery\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = Gambetta,
                                        fontStyle = FontStyle.Italic
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(entries, key = { it.entry.id }) { entryWithSenses ->
                            DictionaryEntryItem(
                                entryWithSenses = entryWithSenses,
                                showLemmaNote = hasLemmatization
                            )
                        }
                        if (isBrowsingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

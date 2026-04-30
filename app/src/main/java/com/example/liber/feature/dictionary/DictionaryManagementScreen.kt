package com.example.liber.feature.dictionary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Book
import com.adamglin.phosphoricons.regular.FileArrowUp
import com.adamglin.phosphoricons.regular.X
import com.example.liber.R
import com.example.liber.core.designsystem.AppErrorState
import com.example.liber.core.designsystem.EditorialSearchField
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.designsystem.LiberCollapsingScreen
import com.example.liber.core.designsystem.LiberTabBar
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Dictionary
import com.example.liber.data.model.FreeDictCatalogItem
import com.example.liber.feature.dictionary.components.CreateDictionaryDialog
import com.example.liber.feature.dictionary.components.EditorialDictionaryItem
import com.example.liber.feature.dictionary.components.EditorialFilterSentence
import com.example.liber.feature.dictionary.components.RenameDictionaryDialog


@Composable
fun DictionaryManagementScreen(
    viewModel: DictionaryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentScreen by remember { mutableStateOf("manager") } // "manager" | "viewer"
    var selectedDictionary by remember { mutableStateOf<Dictionary?>(null) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == "viewer") {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            } else {
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            }
        },
        label = "dictionary_screen_transition"
    ) { screen ->
        when (screen) {
            "manager" -> {
                DictionaryManagerContent(
                    viewModel = viewModel,
                    onBack = onBack,
                    onOpenDictionary = {
                        selectedDictionary = it
                        viewModel.browseDictionary(it.id)
                        currentScreen = "viewer"
                    },
                    modifier = modifier
                )
            }

            "viewer" -> {
                selectedDictionary?.let { dict ->
                    DictionaryViewerScreen(
                        dictionary = dict,
                        viewModel = viewModel,
                        onBack = {
                            currentScreen = "manager"
                            viewModel.clearBrowseState()
                        },
                        modifier = modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun DictionaryManagerContent(
    viewModel: DictionaryViewModel,
    onBack: () -> Unit,
    onOpenDictionary: (Dictionary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dictionaries by viewModel.dictionaries.collectAsState()
    val catalogState by viewModel.freeDictCatalogState.collectAsState()
    val downloadingCodes by viewModel.downloadingCodes.collectAsState()
    val lemmatizationStatus by viewModel.lemmatizationStatus.collectAsState()
    val languagesWithLemmas by viewModel.languagesWithLemmas.collectAsState()

    var activeTab by remember { mutableStateOf("installed") } // "installed" | "available"
    var searchQuery by remember { mutableStateOf("") }
    var filterFrom by remember { mutableStateOf("All") }
    var filterTo by remember { mutableStateOf("All") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Dictionary?>(null) }

    val sourceLanguages = remember(catalogState) {
        if (catalogState is UiState.Success) {
            val list = (catalogState as UiState.Success<List<FreeDictCatalogItem>>).data
            listOf("All") + list.map { getLanguageName(it.sourceLanguageTag) }.distinct().sorted()
        } else listOf("All")
    }

    val targetLanguages = remember(catalogState) {
        if (catalogState is UiState.Success) {
            val list = (catalogState as UiState.Success<List<FreeDictCatalogItem>>).data
            listOf("All") + list.map { getLanguageName(it.targetLanguageTag) }.distinct().sorted()
        } else listOf("All")
    }

    LiberCollapsingScreen(
        title = UiText.StringResource(R.string.settings_dictionary_title),
        modifier = modifier,
        onBack = onBack,
        headerActions = {
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = PhosphorIcons.Regular.FileArrowUp,
                    contentDescription = stringResource(R.string.settings_dictionary_action_add_manual),
                )
            }
        }
    ) {
        val tabTitles = listOf(
            UiText.DynamicString("Installed (${dictionaries.size})"),
            UiText.DynamicString("FreeDict API")
        )
        val tabIds = listOf("installed", "available")

        LiberTabBar(
            tabs = tabTitles,
            selectedTabIndex = tabIds.indexOf(activeTab).coerceAtLeast(0),
            onTabSelected = { index -> activeTab = tabIds[index] },
            modifier = Modifier.padding(top = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            item {
                // SEARCH BAR / FILTERS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 16.dp)
                ) {
                    if (activeTab == "available") {
                        EditorialSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search languages...",
                            modifier = Modifier.fillMaxWidth()
                        )

                        EditorialFilterSentence(
                            filterFrom = filterFrom,
                            onFromChange = { filterFrom = it },
                            filterTo = filterTo,
                            onToChange = { filterTo = it },
                            sourceLanguages = sourceLanguages,
                            targetLanguages = targetLanguages,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        )
                    } else {
                        EditorialSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Filter installed...",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (activeTab == "installed") {
                val filteredDicts = dictionaries.filter {
                    (it.displayName.contains(searchQuery, ignoreCase = true) ||
                            (it.localAlias?.contains(searchQuery, ignoreCase = true) ?: false))
                }.sortedWith { a, b ->
                    if (searchQuery.isNotEmpty()) {
                        val aSourceMatches = getLanguageName(a.sourceLanguageTag).contains(
                            searchQuery,
                            ignoreCase = true
                        )
                        val bSourceMatches = getLanguageName(b.sourceLanguageTag).contains(
                            searchQuery,
                            ignoreCase = true
                        )
                        if (aSourceMatches != bSourceMatches) {
                            return@sortedWith if (aSourceMatches) -1 else 1
                        }
                    }
                    0 // Keep original order otherwise
                }
                if (filteredDicts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isEmpty()) PhosphorIcons.Regular.Book else PhosphorIcons.Regular.X,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "No dictionaries installed yet." else "No matches found.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Gambetta,
                                    fontStyle = FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredDicts, key = { it.id }) { dictionary ->
                        val normalizedLang =
                            viewModel.normalizeLanguageTag(dictionary.sourceLanguageTag)
                        val isLemmatizing = lemmatizationStatus.containsKey(normalizedLang)
                        val hasLemma = languagesWithLemmas.contains(normalizedLang)

                        EditorialDictionaryItem(
                            title = dictionary.localAlias
                                ?: (getLanguageName(dictionary.sourceLanguageTag) + " to " + (dictionary.targetLanguageTag?.let {
                                    getLanguageName(it)
                                } ?: "Self")),
                            subtitle = dictionary.id,
                            version = dictionary.version,
                            size = formatBytes(dictionary.installSizeBytes),
                            hasLemmatization = hasLemma || isLemmatizing,
                            onClick = { onOpenDictionary(dictionary) },
                            onDelete = { viewModel.deleteDictionary(dictionary.id) }
                        )
                    }
                }
            } else {
                // AVAILABLE TAB
                when (catalogState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                    }

                    is UiState.Error -> {
                        item {
                            val errorState = catalogState as UiState.Error
                            AppErrorState(
                                title = errorState.title,
                                message = errorState.message,
                                onRetry = viewModel::refreshFreeDictCatalog,
                                fillMaxSize = false,
                            )
                        }
                    }

                    is UiState.Success -> {
                        val catalog =
                            (catalogState as UiState.Success<List<FreeDictCatalogItem>>).data
                        val filtered = catalog.filter {
                            val matchSearch =
                                it.code.contains(searchQuery, ignoreCase = true) ||
                                        getLanguageName(it.sourceLanguageTag).contains(
                                            searchQuery,
                                            ignoreCase = true
                                        ) ||
                                        getLanguageName(it.targetLanguageTag).contains(
                                            searchQuery,
                                            ignoreCase = true
                                        )

                            val matchFrom =
                                filterFrom == "All" || getLanguageName(it.sourceLanguageTag) == filterFrom
                            val matchTo =
                                filterTo == "All" || getLanguageName(it.targetLanguageTag) == filterTo

                            matchSearch && matchFrom && matchTo
                        }.sortedWith { a, b ->
                            if (searchQuery.isNotEmpty()) {
                                val aSourceMatches = getLanguageName(a.sourceLanguageTag)
                                    .contains(searchQuery, ignoreCase = true)
                                val bSourceMatches = getLanguageName(b.sourceLanguageTag)
                                    .contains(searchQuery, ignoreCase = true)
                                if (aSourceMatches != bSourceMatches) {
                                    return@sortedWith if (aSourceMatches) -1 else 1
                                }
                            }
                            val sourceCmp = getLanguageName(a.sourceLanguageTag)
                                .compareTo(getLanguageName(b.sourceLanguageTag))
                            if (sourceCmp != 0) return@sortedWith sourceCmp
                            getLanguageName(a.targetLanguageTag)
                                .compareTo(getLanguageName(b.targetLanguageTag))
                        }

                        if (filtered.isEmpty()) {
                            item {
                                Text(
                                    text = "No dictionaries found for this combination.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = Gambetta,
                                        fontStyle = FontStyle.Italic
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 64.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            items(filtered, key = { it.code }) { item ->
                                val isDownloading = downloadingCodes.contains(item.code)
                                val isInstalled =
                                    dictionaries.any { it.id == "freedict-${item.code}" }

                                val normalizedLang =
                                    viewModel.normalizeLanguageTag(item.sourceLanguageTag)
                                val isLemmatizing =
                                    lemmatizationStatus.containsKey(normalizedLang)
                                val hasLemma = languagesWithLemmas.contains(normalizedLang)

                                EditorialDictionaryItem(
                                    title = getLanguageName(item.sourceLanguageTag) + " to " + getLanguageName(
                                        item.targetLanguageTag
                                    ),
                                    subtitle = item.code,
                                    version = item.version,
                                    wordsCount = item.headwords,
                                    size = formatBytes(item.stardictSizeBytes),
                                    hasLemmatization = hasLemma || isLemmatizing,
                                    isInstalled = isInstalled,
                                    isDownloading = isDownloading,
                                    onDownload = { viewModel.downloadFreeDict(item) },
                                    onClick = { /* No-op for not installed */ }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showCreateDialog) {
        CreateDictionaryDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, source, target, type, uri ->
                viewModel.createDictionary(name, source, target, type, uri)
                showCreateDialog = false
            },
        )
    }

    renameTarget?.let { dictionary ->
        RenameDictionaryDialog(
            dictionary = dictionary,
            onDismiss = { renameTarget = null },
            onConfirm = {
                viewModel.renameDictionary(dictionary.id, it)
                renameTarget = null
            }
        )
    }
}

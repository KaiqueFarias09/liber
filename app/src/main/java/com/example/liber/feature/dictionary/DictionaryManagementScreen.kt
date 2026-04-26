package com.example.liber.feature.dictionary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Book
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.DownloadSimple
import com.adamglin.phosphoricons.regular.FileArrowUp
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.Sparkle
import com.adamglin.phosphoricons.regular.Trash
import com.adamglin.phosphoricons.regular.X
import com.example.liber.R
import com.example.liber.core.designsystem.AppErrorState
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.designsystem.LiberDialog
import com.example.liber.core.designsystem.LiberScreen
import com.example.liber.core.designsystem.LiberTabBar
import com.example.liber.core.designsystem.LiberTextField
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.local.DictionaryEntryWithSenses
import com.example.liber.data.model.Dictionary
import com.example.liber.data.model.FreeDictCatalogItem
import com.example.liber.feature.dictionary.components.DictionaryEntryItem
import java.util.Locale

private val languageMap = mapOf(
    "afr" to "Afrikaans", "deu" to "German", "eng" to "English",
    "ara" to "Arabic", "bre" to "Breton", "fra" to "French",
    "cat" to "Catalan", "fin" to "Finnish"
)

private fun getLanguageName(tag: String): String {
    return languageMap[tag] ?: try {
        Locale.forLanguageTag(tag).getDisplayLanguage(Locale.ENGLISH)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            .ifEmpty { tag.uppercase(Locale.ROOT) }
    } catch (e: Exception) {
        tag.uppercase(Locale.ROOT)
    }
}

private fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes == 0L) return "Unknown size"
    val k = 1024.0
    val sizes = arrayOf("B", "KB", "MB", "GB")
    val i = Math.floor(Math.log(bytes.toDouble()) / Math.log(k)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(k, i.toDouble()), sizes[i])
}

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

    LiberScreen(
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
        Column(modifier = Modifier.fillMaxSize()) {
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

            // SEARCH BAR / FILTERS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f))
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

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (activeTab == "installed") {
                    val filteredDicts = dictionaries.filter {
                        (it.displayName.contains(searchQuery, ignoreCase = true) ||
                                (it.localAlias?.contains(searchQuery, ignoreCase = true) ?: false))
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

@Composable
private fun DictionaryViewerScreen(
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

    LiberScreen(
        title = UiText.DynamicString(displayTitle),
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (showSmartInfo) {
                // Info block for lemmatization
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

            when (browseState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }

                is UiState.Error -> {
                    val errorState = browseState as UiState.Error
                    AppErrorState(
                        title = errorState.title,
                        message = errorState.message,
                        onRetry = { viewModel.browseDictionary(dictionary.id, browseQuery) },
                        fillMaxSize = false,
                    )
                }

                is UiState.Success -> {
                    val entries =
                        (browseState as UiState.Success<List<DictionaryEntryWithSenses>>).data
                    if (entries.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
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
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
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
                            item { Spacer(Modifier.height(32.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    val borderColor =
        if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val strokeWidth = if (isFocused) 2.dp else 1.dp

    Box(
        modifier = modifier
            .padding(vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusRequester.requestFocus() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val h = this.size.height
                    val w = this.size.width
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = strokeWidth.toPx()
                    )
                }
                .padding(horizontal = 24.dp) // Added horizontal padding inside the row so the line is full width
                .padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = PhosphorIcons.Regular.MagnifyingGlass,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.5f
                )
            )
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Gambetta,
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    interactionSource = interactionSource,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Gambetta,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        }
    }
}

@Composable
private fun EditorialFilterSentence(
    filterFrom: String,
    onFromChange: (String) -> Unit,
    filterTo: String,
    onToChange: (String) -> Unit,
    sourceLanguages: List<String>,
    targetLanguages: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Show dictionaries from",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Gambetta),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        EditorialDropdown(
            value = filterFrom,
            onValueChange = onFromChange,
            options = sourceLanguages
        )

        Text(
            text = "to",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Gambetta),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        EditorialDropdown(
            value = filterTo,
            onValueChange = onToChange,
            options = targetLanguages
        )

        Text(
            text = ".",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Gambetta),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditorialDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    val accentColor = MaterialTheme.colorScheme.primary

    Box {
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontFamily = Gambetta
            ),
            color = accentColor,
            modifier = Modifier
                .clickable { expanded = true }
                .drawBehind {
                    val h = this.size.height
                    val w = this.size.width
                    drawLine(
                        color = accentColor.copy(alpha = 0.5f),
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    )
                }
                .padding(bottom = 2.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .sizeIn(maxHeight = 280.dp) // Height constraint for the popup
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Gambetta)
                        )
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EditorialDictionaryItem(
    title: String,
    subtitle: String,
    size: String,
    hasLemmatization: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    isInstalled: Boolean = true,
    isDownloading: Boolean = false,
    onDownload: (() -> Unit)? = null,
    wordsCount: Int? = null,
    version: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .drawBehind {
                val h = this.size.height
                val w = this.size.width
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 24.dp) // Added horizontal padding inside Column so the line is full width
            .padding(
                top = 20.dp,
                bottom = 24.dp
            ) // Padding is now AFTER drawBehind, creating space above the line
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val parts = title.split(" to ")
                if (parts.size == 2) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = parts[0],
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = Gambetta
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " to ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        Text(
                            text = parts[1],
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = Gambetta
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = Gambetta
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "$subtitle ${if (version != null) "• v$version" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (isInstalled && onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Trash,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            } else if (onDownload != null) {
                IconButton(
                    onClick = onDownload,
                    enabled = !isDownloading,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = PhosphorIcons.Regular.DownloadSimple,
                            contentDescription = "Download",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (version != null && !isInstalled) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = "v$version",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                if (wordsCount != null) {
                    Text(
                        text = String.format("%,d words", wordsCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }

                Text(
                    text = size,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (hasLemmatization) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Sparkle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Smart",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (isInstalled) {
                Icon(
                    imageVector = PhosphorIcons.Regular.CaretRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun CreateDictionaryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, source: String, target: String?, type: String, uri: Uri?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var sourceTag by remember { mutableStateOf("en") }
    var targetTag by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("monolingual") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = DocumentFile.fromSingleUri(context, uri)?.name ?: "file"
            val extension = fileName.lowercase()
            val supportedExtensions =
                listOf(".tar.xz", ".tar.gz", ".zip", ".tar", ".dz", ".ifo", ".idx", ".dict")

            if (supportedExtensions.any { ext -> extension.endsWith(ext) }) {
                selectedUri = uri
                selectedFileName = fileName
                fileError = null
            } else {
                selectedUri = null
                selectedFileName = null
                fileError = context.getString(R.string.settings_dictionary_error_unsupported_file)
            }
        }
    }

    var typeMenuExpanded by remember { mutableStateOf(false) }

    LiberDialog(
        onDismissRequest = onDismiss,
        title = UiText.StringResource(R.string.settings_dictionary_create_title),
        dismissLabel = UiText.StringResource(R.string.action_cancel),
        confirmLabel = UiText.StringResource(R.string.action_save),
        confirmEnabled = name.trim().isNotEmpty() && sourceTag.trim()
            .isNotEmpty() && selectedUri != null,
        onConfirm = {
            onConfirm(
                name.trim(),
                sourceTag.trim(),
                targetTag.trim().ifEmpty { null },
                type,
                selectedUri
            )
        },
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // File Picker Row
            Surface(
                onClick = { launcher.launch("*/*") },
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.FileArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_dictionary_field_file),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (fileError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedFileName
                                ?: stringResource(R.string.settings_dictionary_field_file_select),
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                fileError != null -> MaterialTheme.colorScheme.error
                                selectedFileName != null -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                        if (fileError != null) {
                            Text(
                                text = fileError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            LiberTextField(
                value = name,
                onValueChange = { name = it },
                label = UiText.StringResource(R.string.field_label_name),
                singleLine = true,
            )
            LiberTextField(
                value = sourceTag,
                onValueChange = { sourceTag = it },
                label = UiText.StringResource(R.string.settings_dictionary_field_source_language),
                singleLine = true,
            )
            LiberTextField(
                value = targetTag,
                onValueChange = { targetTag = it },
                label = UiText.StringResource(R.string.settings_dictionary_field_target_language),
                singleLine = true,
            )

            // Type Dropdown
            Box {
                LiberTextField(
                    value = if (type == "monolingual") stringResource(R.string.settings_dictionary_type_monolingual) else stringResource(
                        R.string.settings_dictionary_type_bilingual
                    ),
                    onValueChange = {},
                    label = UiText.StringResource(R.string.settings_dictionary_field_type),
                    readOnly = true,
                    enabled = true,
                    trailingIcon = {
                        IconButton(onClick = { typeMenuExpanded = true }) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.CaretDown,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.clickable { typeMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_dictionary_type_monolingual)) },
                        onClick = {
                            type = "monolingual"
                            typeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_dictionary_type_bilingual)) },
                        onClick = {
                            type = "bilingual"
                            typeMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDictionaryDialog(
    dictionary: Dictionary,
    onDismiss: () -> Unit,
    onConfirm: (alias: String?) -> Unit,
) {
    var alias by remember { mutableStateOf(dictionary.localAlias ?: dictionary.displayName) }

    LiberDialog(
        onDismissRequest = onDismiss,
        title = UiText.StringResource(R.string.settings_dictionary_rename_title),
        dismissLabel = UiText.StringResource(R.string.action_cancel),
        confirmLabel = UiText.StringResource(R.string.action_save),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(alias.trim().ifEmpty { null }) },
    ) {
        LiberTextField(
            value = alias,
            onValueChange = { alias = it },
            label = UiText.StringResource(R.string.field_label_name),
            singleLine = true,
        )
    }
}

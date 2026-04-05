package com.example.liber.ui.reader

import android.app.Application
import android.view.ActionMode
import com.example.liber.R
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Bookmark
import com.adamglin.phosphoricons.regular.List
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.Minus
import com.adamglin.phosphoricons.regular.NotePencil
import com.adamglin.phosphoricons.regular.Plus
import androidx.compose.foundation.text.BasicTextField
import com.adamglin.phosphoricons.regular.TextAa
import com.adamglin.phosphoricons.regular.Trash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.withStarted
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.liber.data.AnnotationEntity
import com.example.liber.data.BookmarkEntity
import com.example.liber.ui.components.EmptyState
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val ID_HIGHLIGHT = 9_001
private const val ID_ADD_NOTE  = 9_002

// Design constants
private val ModalBg     = Color(0xFF1C1C1E)
private val ModalItemBg = Color(0xFF2C2C2E)
private val GreenAccent = Color(0xFF32D74B)
private val RedAccent   = Color(0xFFFF3B30)
private val CyanAccent  = Color(0xFF64D2FF)

/** Recursively finds the first [WebView] in the view hierarchy, or null. */
private fun findWebView(view: View?): WebView? {
    if (view is WebView) return view
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            findWebView(view.getChildAt(i))?.let { return it }
        }
    }
    return null
}

// Pastel highlight color options (ARGB Int) — mirrors the extended palette light containers
private val AnnotationColorOptions = listOf(
    0xFFFFF8DC.toInt(), // Yellow
    0xFFFFEDD8.toInt(), // Orange
    0xFFFFE4E8.toInt(), // Rose
    0xFFDCE8FF.toInt(), // Blue
    0xFFDCF2E0.toInt(), // Green
    0xFFEEDFFF.toInt(), // Purple
    0xFFCEF5F0.toInt(), // Teal
    0xFFFFDEDE.toInt(), // Red
)

/**
 * Full-screen EPUB reader with a design matching the app's iOS-inspired aesthetic.
 * Features: theme switching (persisted), bookmarks, annotations, search, table of contents.
 */
@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    publication: Publication,
    bookId: String,
    initialLocatorJson: String?,
    annotations: List<AnnotationEntity>,
    bookmarks: List<BookmarkEntity>,
    pendingAnnotationRequest: AnnotationRequest?,
    onRequestAnnotation: (AnnotationRequest) -> Unit,
    onSaveLocator: (json: String, progress: Int) -> Unit,
    onSaveAnnotation: (AnnotationEntity) -> Unit,
    onDeleteAnnotation: (Long) -> Unit,
    onSaveBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onClearPendingAnnotation: () -> Unit,
    onBack: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ReaderViewModel = viewModel(
        factory = ReaderViewModel.Factory(application, publication)
    )

    val fragmentActivity = LocalActivity.current as FragmentActivity
    val coroutineScope = rememberCoroutineScope()

    val showUI        by viewModel.showUI.collectAsState()
    val themeId       by viewModel.themeId.collectAsState()
    val fontSize      by viewModel.fontSize.collectAsState()
    val showAnnotationCreator by viewModel.showAnnotationCreator.collectAsState()

    val theme = findReaderTheme(themeId)

    // Modal visibility state
    var showContents  by remember { mutableStateOf(false) }
    var showSearch    by remember { mutableStateOf(false) }
    var showNotebook  by remember { mutableStateOf(false) }
    var showThemes    by remember { mutableStateOf(false) }

    val isAnyModalOpen = showContents || showSearch || showNotebook || showThemes || showAnnotationCreator

    // Bridge: when the selection callback posts a text-selection request, open the annotation creator
    LaunchedEffect(pendingAnnotationRequest) {
        pendingAnnotationRequest ?: return@LaunchedEffect
        val type = if (pendingAnnotationRequest is AnnotationRequest.Highlight) "highlight" else "note"
        viewModel.startAnnotation(type = type, prefilledText = pendingAnnotationRequest.selectedText)
        onClearPendingAnnotation()
    }

    var navigator by remember { mutableStateOf<VisualNavigator?>(null) }

    // Toggle UI on tap using Readium's InputListener
    LaunchedEffect(navigator) {
        val nav = navigator ?: return@LaunchedEffect
        val listener = object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                // Determine if it's a center tap (toggle UI) or edge tap (page turn)
                val width = navigator?.publicationView?.width ?: 0
                val x = event.point.x
                
                when {
                    x < width * 0.2f -> {
                        (nav as? OverflowableNavigator)?.goBackward(animated = true)
                    }
                    x > width * 0.8f -> {
                        (nav as? OverflowableNavigator)?.goForward(animated = true)
                    }
                    else -> {
                        viewModel.toggleUI()
                    }
                }
                return true
            }
        }
        nav.addInputListener(listener)
    }

    // Track current locator for the progress scrubber and bookmark detection
    var currentLocator by remember { mutableStateOf<Locator?>(null) }
    LaunchedEffect(navigator) {
        navigator?.currentLocator?.collect { currentLocator = it }
    }

    val progress = currentLocator?.locations?.totalProgression?.toFloat() ?: 0f

    // Determine if the current position is already bookmarked (same chapter + close progression)
    val isCurrentPageBookmarked = currentLocator?.let { cl ->
        bookmarks.any { bm ->
            runCatching {
                val bmLoc = Locator.fromJSON(JSONObject(bm.locator)) ?: return@runCatching false
                bmLoc.href == cl.href &&
                    abs((bmLoc.locations.progression ?: 0.0) - (cl.locations.progression ?: 0.0)) < 0.03
            }.getOrDefault(false)
        }
    } ?: false

    // Render stored annotations as highlights via the Decorator API
    LaunchedEffect(navigator, annotations) {
        val dn = navigator as? DecorableNavigator ?: return@LaunchedEffect
        val fragment = navigator as? Fragment ?: return@LaunchedEffect
        val decorations = annotations.mapNotNull { annotation ->
            runCatching {
                val locator = Locator.fromJSON(JSONObject(annotation.locator))
                    ?: return@runCatching null
                Decoration(
                    id = annotation.id.toString(),
                    locator = locator,
                    style = Decoration.Style.Highlight(tint = annotation.color),
                )
            }.getOrNull()
        }
        fragment.lifecycle.withStarted { }
        dn.applyDecorations(decorations, "annotations")
    }

    // Apply theme + font size preferences whenever they change or the navigator becomes available.
    // Keys include `navigator` so this re-runs when the fragment is first created.
    // `withStarted {}` mirrors the decoration pattern — it suspends until the fragment is STARTED
    // (i.e. attached to the activity), preventing the "not attached" crash.
    LaunchedEffect(navigator, themeId, fontSize) {
        val epubFrag = navigator as? EpubNavigatorFragment ?: return@LaunchedEffect
        epubFrag.lifecycle.withStarted { }
        val t = findReaderTheme(themeId)
        epubFrag.submitPreferences(
            EpubPreferences(
                backgroundColor = ReadiumColor(t.background.toArgb()),
                textColor = ReadiumColor(t.textColor.toArgb()),
                publisherStyles = false,
                fontSize = fontSize,
            )
        )
    }

    val handleBack = {
        currentLocator?.let { locator ->
            val prog = ((locator.locations.totalProgression ?: 0.0) * 100).toInt()
            onSaveLocator(locator.toJSON().toString(), prog)
        }
        onBack()
    }

    BackHandler(onBack = handleBack)

    // UI chrome colors derived from theme
    val chromeBg     = if (theme.isDark) Color(0xFF111111).copy(alpha = 0.95f)
                       else Color(0xFFF4F4F0).copy(alpha = 0.95f)
    val chromeIcon   = if (theme.isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)
    val chromeOnIcon = if (theme.isDark) Color.White else Color.Black
    val chromeDivider = if (theme.isDark) Color(0xFF2C2C2E) else Color(0xFFD1D1D6)
    val chromeLabel  = if (theme.isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background),
    ) {
        // ── EPUB rendering via Readium ────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = View.generateViewId()

                    val restoredLocator = initialLocatorJson?.let { json ->
                        runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()
                    }

                    val selectionCallback = object : ActionMode.Callback {
                        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                            menu.add(Menu.NONE, ID_HIGHLIGHT, 0, context.getString(R.string.action_highlight))
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                            menu.add(Menu.NONE, ID_ADD_NOTE, 1, context.getString(R.string.action_add_note))
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                            return true
                        }
                        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                            if (menu.findItem(ID_HIGHLIGHT) == null)
                                menu.add(Menu.NONE, ID_HIGHLIGHT, 0, context.getString(R.string.action_highlight))
                                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                            if (menu.findItem(ID_ADD_NOTE) == null)
                                menu.add(Menu.NONE, ID_ADD_NOTE, 1, context.getString(R.string.action_add_note))
                                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                            return true
                        }
                        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                            if (item.itemId != ID_HIGHLIGHT && item.itemId != ID_ADD_NOTE) return false
                            val webView = findWebView(fragmentActivity.window.decorView)
                            webView?.evaluateJavascript("window.getSelection().toString()") { raw ->
                                val text = raw?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
                                val request = if (item.itemId == ID_HIGHLIGHT)
                                    AnnotationRequest.Highlight(text)
                                else
                                    AnnotationRequest.Note(text)
                                onRequestAnnotation(request)
                                mode.finish()
                            } ?: mode.finish()
                            return true
                        }
                        override fun onDestroyActionMode(mode: ActionMode) {}
                    }

                    val initialTheme = findReaderTheme(viewModel.themeId.value)
                    val initialPrefs = EpubPreferences(
                        backgroundColor = ReadiumColor(initialTheme.background.toArgb()),
                        textColor = ReadiumColor(initialTheme.textColor.toArgb()),
                        publisherStyles = false,
                        fontSize = viewModel.fontSize.value,
                    )

                    val navigatorFactory = EpubNavigatorFactory(publication)
                    val fragmentFactory = navigatorFactory.createFragmentFactory(
                        initialLocator = restoredLocator
                            ?: publication.readingOrder.firstOrNull()
                                ?.let { publication.locatorFromLink(it) },
                        initialPreferences = initialPrefs,
                        configuration = EpubNavigatorFragment.Configuration(
                            decorationTemplates = HtmlDecorationTemplates.defaultTemplates(),
                            selectionActionModeCallback = selectionCallback,
                        ),
                    )
                    fragmentActivity.supportFragmentManager.fragmentFactory = fragmentFactory

                    val navigatorFragment = fragmentActivity.supportFragmentManager
                        .fragmentFactory
                        .instantiate(
                            context.classLoader,
                            "org.readium.r2.navigator.epub.EpubNavigatorFragment",
                        )
                    navigator = navigatorFragment as? VisualNavigator
                    fragmentActivity.supportFragmentManager.commit {
                        replace(id, navigatorFragment)
                    }
                }
            },
        )

        // ── Top Bar ───────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showUI && !isAnyModalOpen,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Surface(
                color = chromeBg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Back button
                    IconButton(onClick = handleBack) {
                        Icon(
                            PhosphorIcons.Regular.ArrowLeft,
                            contentDescription = "Back",
                            tint = chromeOnIcon,
                        )
                    }

                    // Book title + chapter
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = publication.metadata.title ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = chromeLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val chapterTitle = currentLocator?.title
                            ?: currentLocator?.href?.toString()
                                ?.substringAfterLast('/')?.substringBefore('.')
                            ?: ""
                        if (chapterTitle.isNotBlank()) {
                            Text(
                                text = chapterTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = chromeOnIcon,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Theme + Bookmark
                    Row {
                        IconButton(onClick = { showThemes = true }) {
                            Icon(
                                PhosphorIcons.Regular.TextAa,
                                contentDescription = "Themes",
                                tint = chromeIcon,
                            )
                        }
                        IconButton(
                            onClick = {
                                if (isCurrentPageBookmarked) {
                                    val match = bookmarks.find { bm ->
                                        runCatching {
                                            val bmLoc = Locator.fromJSON(JSONObject(bm.locator))
                                                ?: return@runCatching false
                                            bmLoc.href == currentLocator?.href &&
                                                abs((bmLoc.locations.progression ?: 0.0) -
                                                    (currentLocator?.locations?.progression ?: 0.0)) < 0.03
                                        }.getOrDefault(false)
                                    }
                                    match?.let { onDeleteBookmark(it.id) }
                                } else {
                                    currentLocator?.let { loc ->
                                        val chapter = loc.title
                                            ?: loc.href.toString()
                                                .substringAfterLast('/').substringBefore('.')
                                        onSaveBookmark(
                                            BookmarkEntity(
                                                bookId = bookId,
                                                locator = loc.toJSON().toString(),
                                                chapter = chapter,
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                PhosphorIcons.Regular.Bookmark,
                                contentDescription = if (isCurrentPageBookmarked) "Remove bookmark" else "Add bookmark",
                                tint = if (isCurrentPageBookmarked) RedAccent else chromeIcon,
                            )
                        }
                    }
                }
                // Bottom divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .align(Alignment.BottomCenter)
                        .background(chromeDivider)
                )
            }
        }

        // ── Bottom Bar ────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showUI && !isAnyModalOpen,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                color = chromeBg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                ) {
                    // Top divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(chromeDivider)
                    )

                    // Progress scrubber
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = chromeLabel,
                        )
                        ProgressScrubber(
                            progress = progress,
                            isDark = theme.isDark,
                            onProgressChange = { newProg ->
                                viewModel.locatorAtProgress(newProg.toDouble())?.let { locator ->
                                    navigator?.go(locator, animated = false)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.labelSmall,
                            color = chromeLabel,
                        )
                    }

                    // Navigation icons row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        ReaderNavItem(
                            icon = { Icon(PhosphorIcons.Regular.List, contentDescription = null, tint = chromeIcon) },
                            label = "Contents",
                            labelColor = chromeLabel,
                            onClick = { showContents = true },
                        )
                        ReaderNavItem(
                            icon = { Icon(PhosphorIcons.Regular.MagnifyingGlass, contentDescription = null, tint = chromeIcon) },
                            label = "Search",
                            labelColor = chromeLabel,
                            onClick = { showSearch = true },
                        )
                        ReaderNavItem(
                            icon = {
                                Box {
                                    Icon(
                                        PhosphorIcons.Regular.NotePencil,
                                        contentDescription = null,
                                        tint = chromeIcon
                                    )
                                    if (bookmarks.isNotEmpty() || annotations.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .align(Alignment.TopEnd)
                                                .background(GreenAccent, CircleShape)
                                        )
                                    }
                                }
                            },
                            label = "Notebook",
                            labelColor = chromeLabel,
                            onClick = { showNotebook = true },
                        )
                    }
                }
            }
        }
    }

    // ── Table of Contents ─────────────────────────────────────────────────────
    if (showContents) {
        ModalBottomSheet(
            onDismissRequest = { showContents = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ModalBg,
            contentColor = Color.White,
        ) {
            DarkSheetHeader(title = "Contents", onClose = { showContents = false })
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            ) {
                items(publication.tableOfContents) { link ->
                    ContentsRow(link = link, depth = 0, onLinkClick = { clicked ->
                        val locator = publication.locatorFromLink(clicked)
                        if (locator != null) navigator?.go(locator, animated = true)
                        showContents = false
                        viewModel.toggleUI()
                    })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    if (showSearch) {
        ModalBottomSheet(
            onDismissRequest = { showSearch = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ModalBg,
            contentColor = Color.White,
        ) {
            DarkSheetHeader(title = "Search in Book", onClose = { showSearch = false })
            SearchView(
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
                onResultClick = { locator ->
                    navigator?.go(locator, animated = true)
                    showSearch = false
                    viewModel.toggleUI()
                },
            )
        }
    }

    // ── Notebook (Bookmarks, Highlights & Notes) ─────────────────────────────
    if (showNotebook) {
        ModalBottomSheet(
            onDismissRequest = { showNotebook = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ModalBg,
            contentColor = Color.White,
        ) {
            DarkSheetHeader(title = "Notebook", onClose = { showNotebook = false })
            NotebookView(
                bookmarks = bookmarks,
                annotations = annotations,
                modifier = Modifier.weight(1f),
                onBookmarkClick = { bm ->
                    runCatching {
                        val locator = Locator.fromJSON(JSONObject(bm.locator))
                        if (locator != null) navigator?.go(locator, animated = true)
                    }
                    showNotebook = false
                    viewModel.toggleUI()
                },
                onDeleteBookmark = { bm -> onDeleteBookmark(bm.id) },
                onNoteClick = { annotation ->
                    runCatching {
                        val locator = Locator.fromJSON(JSONObject(annotation.locator))
                        if (locator != null) navigator?.go(locator, animated = true)
                    }
                    showNotebook = false
                    viewModel.toggleUI()
                },
                onDeleteNote = { annotation -> onDeleteAnnotation(annotation.id) },
            )
        }
    }

    // ── Themes & Settings ─────────────────────────────────────────────────────
    if (showThemes) {
        ModalBottomSheet(
            onDismissRequest = { showThemes = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ModalBg,
            contentColor = Color.White,
        ) {
            ThemesSheet(
                currentThemeId = themeId,
                onThemeChange = { id -> viewModel.setTheme(id) },
                onDecreaseFontSize = { viewModel.adjustFontSize(-0.1) },
                onIncreaseFontSize = { viewModel.adjustFontSize(+0.1) },
                onClose = { showThemes = false },
            )
        }
    }

    // ── Create / Edit Annotation ──────────────────────────────────────────────
    if (showAnnotationCreator) {
        val noteText     by viewModel.annotationNoteText.collectAsState()
        val selectedColor by viewModel.annotationColorArgb.collectAsState()
        val selectedText  by viewModel.pendingSelectedText.collectAsState()
        val annotationType by viewModel.pendingAnnotationType.collectAsState()

        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelAnnotation() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ModalBg,
            contentColor = Color.White,
        ) {
            CreateAnnotationSheet(
                annotationType = annotationType,
                selectedText = selectedText,
                noteText = noteText,
                selectedColorArgb = selectedColor,
                currentChapter = navigator?.currentLocator?.value?.title
                    ?: navigator?.currentLocator?.value?.href?.toString(),
                onNoteTextChange = { viewModel.setAnnotationNote(it) },
                onColorChange = { viewModel.setAnnotationColor(it) },
                onSave = {
                    coroutineScope.launch {
                        val selectable = navigator as? SelectableNavigator
                        val locator = selectable?.currentSelection()?.locator
                            ?: navigator?.currentLocator?.value
                            ?: return@launch
                        onSaveAnnotation(
                            AnnotationEntity(
                                bookId = bookId,
                                type = annotationType,
                                color = selectedColor,
                                locator = locator.toJSON().toString(),
                                text = selectedText,
                                note = noteText.ifBlank { null },
                            )
                        )
                        selectable?.clearSelection()
                        viewModel.cancelAnnotation()
                    }
                },
                onCancel = { viewModel.cancelAnnotation() },
            )
        }
    }
}

// ── Progress Scrubber ─────────────────────────────────────────────────────────

@Composable
private fun ProgressScrubber(
    progress: Float,
    isDark: Boolean,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor    = if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6)
    val progressColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)

    // Local state to track the drag position for immediate UI feedback
    var draggingProgress by remember { mutableStateOf<Float?>(null) }
    val displayProgress = draggingProgress ?: progress

    Canvas(
        modifier = modifier
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onProgressChange(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        draggingProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        draggingProgress?.let { onProgressChange(it) }
                        draggingProgress = null
                    },
                    onDragCancel = {
                        draggingProgress = null
                    },
                    onDrag = { change, _ ->
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        draggingProgress = newProgress
                    }
                )
            }
    ) {
        val trackH = 4.dp.toPx()
        val trackY = (size.height - trackH) / 2f
        val clampedProgress = displayProgress.coerceIn(0f, 1f)

        // Background track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, trackY),
            size = Size(size.width, trackH),
            cornerRadius = CornerRadius(trackH / 2f),
        )

        // Filled portion
        val fillW = size.width * clampedProgress
        if (fillW > 0f) {
            drawRoundRect(
                color = progressColor,
                topLeft = Offset(0f, trackY),
                size = Size(fillW, trackH),
                cornerRadius = CornerRadius(trackH / 2f),
            )
        }

        // Thumb
        val thumbR = 10.dp.toPx()
        val thumbX = fillW.coerceIn(thumbR, size.width - thumbR)
        
        // Shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.15f),
            radius = thumbR + 1.dp.toPx(),
            center = Offset(thumbX, size.height / 2f + 1.dp.toPx())
        )
        
        // Body
        drawCircle(
            color = Color.White,
            radius = thumbR,
            center = Offset(thumbX, size.height / 2f)
        )
    }
}

// ── ReaderNavItem ─────────────────────────────────────────────────────────────

@Composable
private fun ReaderNavItem(
    icon: @Composable () -> Unit,
    label: String,
    labelColor: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        icon()
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontSize = 10.sp,
        )
    }
}

// ── DarkSheetHeader ───────────────────────────────────────────────────────────

@Composable
private fun DarkSheetHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(ModalItemBg, CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF3A3A3C)))
}

// ── ContentsView ──────────────────────────────────────────────────────────────

@Composable
private fun ContentsRow(
    link: Link,
    depth: Int,
    onLinkClick: (Link) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLinkClick(link) }
            .padding(
                start = (depth * 20 + 4).dp,
                end = 4.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = link.title ?: "Untitled",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (depth == 0) FontWeight.SemiBold else FontWeight.Normal,
            color = if (depth == 0) Color.White else Color(0xFFAAAAAA),
            modifier = Modifier.weight(1f),
        )
    }
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2C2C2E).copy(alpha = 0.5f)))

    link.children.forEach { child ->
        ContentsRow(link = child, depth = depth + 1, onLinkClick = onLinkClick)
    }
}

// ── SearchView ────────────────────────────────────────────────────────────────

@Composable
fun SearchView(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
    onResultClick: (Locator) -> Unit,
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.search(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = { Text("Search in book...", color = Color(0xFF8E8E93)) },
            leadingIcon = {
                Icon(
                    PhosphorIcons.Regular.MagnifyingGlass,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.search("") }) {
                        Text("✕", color = Color(0xFF8E8E93), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF0A84FF),
                unfocusedBorderColor = Color(0xFF3A3A3C),
                focusedContainerColor = ModalItemBg,
                unfocusedContainerColor = ModalItemBg,
            ),
        )

        if (isSearching && searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF0A84FF), modifier = Modifier.size(32.dp))
            }
        } else if (searchResults.isEmpty() && searchQuery.isNotBlank() && !isSearching) {
            EmptyState(
                title = "No results found",
                subtitle = "Try a different search term.",
                image = R.drawable.search_empty,
                containerColor = Color(0xFF2C2C2E),
                titleColor = Color(0xFF8E8E93),
                subtitleColor = Color(0xFF48484A),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(searchResults) { locator ->
                    SearchResultRow(locator = locator, onClick = { onResultClick(locator) })
                }
                
                item {
                    LaunchedEffect(Unit) {
                        viewModel.loadNextResults()
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    locator: Locator,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        locator.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF0A84FF),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        
        Text(
            text = buildAnnotatedString {
                append(locator.text.before ?: "")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                    append(locator.text.highlight ?: "")
                }
                append(locator.text.after ?: "")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFCCCCCC),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).padding(horizontal = 20.dp).background(Color(0xFF3A3A3C)))
}

// ── NotebookView ─────────────────────────────────────────────────────────────

@Composable
fun NotebookView(
    bookmarks: List<BookmarkEntity>,
    annotations: List<AnnotationEntity>,
    onBookmarkClick: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onNoteClick: (AnnotationEntity) -> Unit,
    onDeleteNote: (AnnotationEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf("bookmarks") }

    Column(modifier = modifier.fillMaxWidth()) {
        // Filter tabs: Bookmarks, Highlights, Notes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(ModalItemBg, RoundedCornerShape(10.dp))
                .padding(4.dp),
        ) {
            listOf("bookmarks" to "Bookmarks", "highlights" to "Highlights", "notes" to "Notes").forEach { (id, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (activeTab == id) ModalBg else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { activeTab = id }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (activeTab == id) Color.White else Color(0xFF8E8E93),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "bookmarks" -> {
                    BookmarksView(
                        bookmarks = bookmarks,
                        onBookmarkClick = onBookmarkClick,
                        onDeleteBookmark = onDeleteBookmark,
                    )
                }
                "highlights" -> {
                    val highlights = remember(annotations) { annotations.filter { it.type == "highlight" } }
                    AnnotationList(
                        annotations = highlights,
                        emptyMessage = "No highlights yet.\nSelect text in the reader to add one.",
                        onNoteClick = onNoteClick,
                        onDeleteNote = onDeleteNote,
                        emptyImage = R.drawable.highlights_empty,
                    )
                }
                "notes" -> {
                    val notes = remember(annotations) { annotations.filter { it.type == "note" } }
                    AnnotationList(
                        annotations = notes,
                        emptyMessage = "No notes yet.\nSelect text in the reader to add one.",
                        onNoteClick = onNoteClick,
                        onDeleteNote = onDeleteNote,
                        emptyImage = R.drawable.notes_empty,
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarksView(
    bookmarks: List<BookmarkEntity>,
    onBookmarkClick: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bookmarks.isEmpty()) {
        EmptyState(
            title = "No bookmarks",
            subtitle = "Tap the bookmark icon to save your place.",
            image = R.drawable.bookmarks_empty,
            containerColor = Color(0xFF2C2C2E),
            titleColor = Color(0xFF8E8E93),
            subtitleColor = Color(0xFF48484A),
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(bookmarks, key = { it.id }) { bm ->
                val dateStr = remember(bm.createdAt) {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(bm.createdAt))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBookmarkClick(bm) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bm.chapter ?: "Unknown chapter",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            PhosphorIcons.Regular.Bookmark,
                            contentDescription = null,
                            tint = Color(0xFF48484A),
                            modifier = Modifier.size(18.dp),
                        )
                        IconButton(
                            onClick = { onDeleteBookmark(bm) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                PhosphorIcons.Regular.Trash,
                                contentDescription = "Delete bookmark",
                                tint = Color(0xFF48484A),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp)
                    .padding(horizontal = 20.dp).background(Color(0xFF2C2C2E).copy(alpha = 0.5f)))
            }
        }
    }
}

@Composable
fun AnnotationList(
    annotations: List<AnnotationEntity>,
    emptyMessage: String,
    onNoteClick: (AnnotationEntity) -> Unit,
    onDeleteNote: (AnnotationEntity) -> Unit,
    @androidx.annotation.DrawableRes emptyImage: Int? = null,
    modifier: Modifier = Modifier,
) {
    if (annotations.isEmpty()) {
        EmptyState(
            title = emptyMessage,
            image = emptyImage,
            containerColor = Color(0xFF2C2C2E),
            titleColor = Color(0xFF8E8E93),
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(annotations, key = { it.id }) { annotation ->
                DarkAnnotationItem(
                    annotation = annotation,
                    onClick = { onNoteClick(annotation) },
                    onDelete = { onDeleteNote(annotation) },
                )
            }
        }
    }
}

@Composable
private fun DarkAnnotationItem(
    annotation: AnnotationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateStr = remember(annotation.createdAt) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(annotation.createdAt))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 20.dp),
    ) {
        // Highlight color left accent border
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(IntrinsicSize.Min)
                .background(Color(annotation.color.toLong() and 0xFFFFFFFFL), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(5.dp))
        Column(modifier = Modifier.weight(1f).padding(top = 2.dp, bottom = 4.dp)) {
            Text(
                text = "Chapter • $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8E8E93),
                letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.height(6.dp))
            if (!annotation.text.isNullOrBlank()) {
                Text(
                    text = "\"${annotation.text}\"",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = Color(0xFFE0E0E0),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (!annotation.note.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ModalItemBg.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .border(0.5.dp, Color(0xFF3A3A3C), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        PhosphorIcons.Regular.NotePencil,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(14.dp).padding(top = 1.dp),
                    )
                    Text(
                        text = annotation.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCCCCCC),
                    )
                }
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                PhosphorIcons.Regular.Trash,
                contentDescription = "Delete",
                tint = Color(0xFF48484A),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ── ThemesSheet ───────────────────────────────────────────────────────────────

@Composable
private fun ThemesSheet(
    currentThemeId: String,
    onThemeChange: (String) -> Unit,
    onDecreaseFontSize: () -> Unit,
    onIncreaseFontSize: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Themes & Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(ModalItemBg, CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Font size row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ModalItemBg, RoundedCornerShape(12.dp)),
        ) {
            // Decrease
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onDecreaseFontSize)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(PhosphorIcons.Regular.Minus, contentDescription = "Decrease font",
                    tint = Color(0xFFE0E0E0), modifier = Modifier.size(20.dp))
            }
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(48.dp)
                    .background(Color(0xFF3A3A3C))
                    .align(Alignment.CenterVertically)
            )
            // Increase
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onIncreaseFontSize)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(PhosphorIcons.Regular.TextAa, contentDescription = "Increase font",
                    tint = Color(0xFFE0E0E0), modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Theme grid (3 columns × 2 rows)
        ReaderThemes.chunked(3).forEach { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowThemes.forEach { t ->
                    ThemePreviewTile(
                        theme = t,
                        isSelected = currentThemeId == t.id,
                        onClick = { onThemeChange(t.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill remaining cells if row is incomplete
                repeat(3 - rowThemes.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ThemePreviewTile(
    theme: ReaderTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .background(theme.background, RoundedCornerShape(16.dp))
            .then(
                if (isSelected)
                    Modifier.border(3.dp, Color(0xFF0A84FF), RoundedCornerShape(16.dp))
                else
                    Modifier.border(1.5.dp, Color(0xFF3A3A3C), RoundedCornerShape(16.dp))
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Aa",
                fontSize = 26.sp,
                fontWeight = if (theme.id == "bold") FontWeight.Bold else FontWeight.Normal,
                color = theme.textColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = theme.name,
                style = MaterialTheme.typography.labelSmall,
                color = theme.textColor.copy(alpha = 0.6f),
            )
        }
    }
}

// ── CreateAnnotationSheet ─────────────────────────────────────────────────────

@Composable
fun CreateAnnotationSheet(
    annotationType: String,
    selectedText: String?,
    noteText: String,
    selectedColorArgb: Int,
    currentChapter: String?,
    onNoteTextChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val isHighlight = annotationType == "highlight"
    val title       = if (isHighlight) "Highlight" else "New Note"
    val saveLabel   = if (isHighlight) "Save highlight" else "Save note"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        currentChapter?.let { chapter ->
            Text(
                text = chapter,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        if (!selectedText.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(
                        color = Color(selectedColorArgb.toLong() and 0xFFFFFFFFL),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = selectedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.8f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }

        Text(
            "Color",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            items(AnnotationColorOptions) { colorArgb ->
                val isSelected = colorArgb == selectedColorArgb
                val color = Color(colorArgb.toLong() and 0xFFFFFFFFL)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color, CircleShape)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF0A84FF) else Color(0xFF3A3A3C),
                            shape = CircleShape,
                        )
                        .clickable { onColorChange(colorArgb) }
                )
            }
        }

        OutlinedTextField(
            value = noteText,
            onValueChange = onNoteTextChange,
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text(if (isHighlight) "Add a note… (optional)" else "Write your note…",
                color = Color(0xFF8E8E93)) },
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF0A84FF),
                unfocusedBorderColor = Color(0xFF3A3A3C),
                focusedContainerColor = ModalItemBg,
                unfocusedContainerColor = ModalItemBg,
            ),
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8E8E93)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A3C)),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
            ) {
                Text(saveLabel, color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}


package com.example.liber.ui.reader

import android.app.Application
import android.content.Intent
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.withStarted
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Bookmark
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.Export
import com.adamglin.phosphoricons.regular.FrameCorners
import com.adamglin.phosphoricons.regular.LineSegments
import com.adamglin.phosphoricons.regular.List
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.Minus
import com.adamglin.phosphoricons.regular.NotePencil
import com.adamglin.phosphoricons.regular.Paragraph
import com.adamglin.phosphoricons.regular.TextAa
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.R
import com.example.liber.data.AnnotationEntity
import com.example.liber.data.BookmarkEntity
import com.example.liber.ui.components.EmptyState
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import org.readium.r2.navigator.preferences.Color as ReadiumColor

private const val ID_HIGHLIGHT = 9_001
private const val ID_ADD_NOTE = 9_002
private const val ID_SHARE = 9_003

// Design constants
internal val GreenAccent = Color(0xFF32D74B)
internal val RedAccent = Color(0xFFFF3B30)

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

// Highlight color options (ARGB Int) — semi-transparent so they're visible on both
// light (Paper, Calm) and dark (Original, Bold, Focus, Quiet) reader themes.
internal val AnnotationColorOptions = listOf(
    0x80FFD60A.toInt(), // Yellow
    0x8030D158.toInt(), // Green
    0x80FF375F.toInt(), // Pink
    0x800A84FF.toInt(), // Blue
    0x80BF5AF2.toInt(), // Purple
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

    val showUI by viewModel.showUI.collectAsState()
    val themeId by viewModel.themeId.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val showAnnotationCreator by viewModel.showAnnotationCreator.collectAsState()
    val showHighlightColorPicker by viewModel.showHighlightColorPicker.collectAsState()
    val tappedAnnotationId by viewModel.tappedAnnotationId.collectAsState()
    // Derive the full entity so the sheet re-renders if the annotation's note changes
    val tappedAnnotation = remember(tappedAnnotationId, annotations) {
        tappedAnnotationId?.let { id -> annotations.find { it.id == id } }
    }

    // New layout / typography settings
    val pageScroll by viewModel.pageScroll.collectAsState()
    val customizeLayout by viewModel.customizeLayout.collectAsState()
    val lineSpacing by viewModel.lineSpacing.collectAsState()
    val characterSpacing by viewModel.characterSpacing.collectAsState()
    val wordSpacing by viewModel.wordSpacing.collectAsState()
    val margins by viewModel.margins.collectAsState()
    val columnCount by viewModel.columnCount.collectAsState()
    val justifyText by viewModel.justifyText.collectAsState()

    val theme = findReaderTheme(themeId)

    // Modal visibility state
    var showContents by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showNotebook by remember { mutableStateOf(false) }
    var showThemes by remember { mutableStateOf(false) }

    val isAnyModalOpen =
        showContents || showSearch || showNotebook || showThemes || showAnnotationCreator || showHighlightColorPicker || (tappedAnnotationId != null)

    // Bridge: highlights → inline color picker; notes → full sheet.
    LaunchedEffect(pendingAnnotationRequest) {
        pendingAnnotationRequest ?: return@LaunchedEffect
        if (pendingAnnotationRequest is AnnotationRequest.Highlight) {
            viewModel.startHighlightColorPicker(pendingAnnotationRequest.selectedText)
        } else {
            viewModel.startAnnotation(
                type = "note",
                prefilledText = pendingAnnotationRequest.selectedText
            )
        }
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

    // Re-inject selection color CSS into Readium's inner iframes on every page turn and theme change.
    // Readium renders each spine item inside a separate <iframe> whose document is replaced on
    // navigation, so a one-time injection is lost. Targeting the iframe's contentDocument via JS
    // ensures the style is applied to the actual EPUB content, not just the outer shell.
    LaunchedEffect(currentLocator?.href, themeId) {
        val t = findReaderTheme(themeId)
        val selCss = t.selectionColorCss
        // Wait briefly for the iframe's content to finish loading after a page turn.
        delay(300)
        val webView = findWebView(fragmentActivity.window.decorView) ?: return@LaunchedEffect

        @Suppress("StringShouldBeRawString")
        val js = """
            (function injectSelectionStyle() {
                var selColor = '$selCss';
                var styleId = '__liber_sel_style__';
                var rule = '::selection { background-color: ' + selColor + ' !important; color: inherit !important; }';

                // Helper: inject/replace style in a given document
                function applyTo(doc) {
                    if (!doc || !doc.head) return;
                    var old = doc.getElementById(styleId);
                    if (old) old.remove();
                    var s = doc.createElement('style');
                    s.id = styleId;
                    s.textContent = rule;
                    doc.head.appendChild(s);
                }

                // Apply to outer document (may not be needed but harmless)
                applyTo(document);

                // Apply to every iframe's inner document — Readium uses iframes per spine item
                var iframes = document.querySelectorAll('iframe');
                for (var i = 0; i < iframes.length; i++) {
                    try {
                        applyTo(iframes[i].contentDocument);
                    } catch (e) { /* cross-origin guard */ }
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    val progress = currentLocator?.locations?.totalProgression?.toFloat() ?: 0f

    // Determine if the current position is already bookmarked (same chapter + close progression)
    val isCurrentPageBookmarked = currentLocator?.let { cl ->
        bookmarks.any { bm ->
            runCatching {
                val bmLoc = Locator.fromJSON(JSONObject(bm.locator)) ?: return@runCatching false
                bmLoc.href == cl.href &&
                        abs(
                            (bmLoc.locations.progression ?: 0.0) - (cl.locations.progression ?: 0.0)
                        ) < 0.03
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

    // Apply theme + font size + layout preferences whenever they change or the navigator becomes available.
    // Keys include `navigator` so this re-runs when the fragment is first created.
    // `withStarted {}` mirrors the decoration pattern — it suspends until the fragment is STARTED
    // (i.e. attached to the activity), preventing the "not attached" crash.
    LaunchedEffect(
        navigator,
        themeId,
        fontSize,
        pageScroll,
        customizeLayout,
        lineSpacing,
        characterSpacing,
        wordSpacing,
        margins,
        columnCount,
        justifyText
    ) {
        val epubFrag = navigator as? EpubNavigatorFragment ?: return@LaunchedEffect
        epubFrag.lifecycle.withStarted { }
        val t = findReaderTheme(themeId)
        val colCount = when (columnCount) {
            "one" -> ColumnCount.ONE
            "two" -> ColumnCount.TWO
            else -> ColumnCount.AUTO
        }
        epubFrag.submitPreferences(
            EpubPreferences(
                backgroundColor = ReadiumColor(t.background.toArgb()),
                textColor = ReadiumColor(t.textColor.toArgb()),
                publisherStyles = false,
                fontSize = fontSize,
                scroll = pageScroll,
                lineHeight = if (customizeLayout) lineSpacing else null,
                // Readium requires letterSpacing >= 0; clamp negative UI values to 0
                letterSpacing = if (customizeLayout) maxOf(0.0, characterSpacing / 100.0) else null,
                wordSpacing = if (customizeLayout) maxOf(0.0, wordSpacing / 100.0) else null,
                pageMargins = if (customizeLayout) maxOf(0.0, 1.0 + margins / 100.0) else null,
                columnCount = colCount,
                textAlign = if (justifyText) TextAlign.JUSTIFY else null,
            )
        )
    }

    // Listen for taps on highlight decorations so we can show the action sheet.
    // Must mirror the withStarted pattern used for applyDecorations — the fragment
    // is not yet attached when `navigator` is first set, so calling addDecorationListener
    // immediately would throw "Fragment not attached to an activity".
    LaunchedEffect(navigator) {
        val dn = navigator as? DecorableNavigator ?: return@LaunchedEffect
        val fragment = navigator as? Fragment ?: return@LaunchedEffect
        val listener = object : DecorableNavigator.Listener {
            override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                val annotationId = event.decoration.id.toLongOrNull() ?: return false
                viewModel.onAnnotationTapped(annotationId)
                return true
            }
        }
        fragment.lifecycle.withStarted { } // suspend until the fragment is attached
        dn.addDecorationListener("annotations", listener)
        try {
            awaitCancellation()            // hold until the effect is cancelled …
        } finally {
            dn.removeDecorationListener(listener) // … then clean up
        }
    }

    val handleBack = {
        currentLocator?.let { locator ->
            val prog = ((locator.locations.totalProgression ?: 0.0) * 100).toInt()
            onSaveLocator(locator.toJSON().toString(), prog)
        }
        onBack()
    }

    BackHandler(onBack = handleBack)

    // UI chrome colors derived from the Material theme
    val colorScheme = MaterialTheme.colorScheme
    val chromeBg = colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
    val chromeIcon = colorScheme.onSurfaceVariant
    val chromeOnIcon = colorScheme.onSurface
    val chromeDivider = colorScheme.outlineVariant
    val chromeLabel = colorScheme.onSurfaceVariant

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
                            menu.add(
                                Menu.NONE,
                                ID_HIGHLIGHT,
                                0,
                                context.getString(R.string.action_highlight)
                            )
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                            menu.add(
                                Menu.NONE,
                                ID_ADD_NOTE,
                                1,
                                context.getString(R.string.action_add_note)
                            )
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                            menu.add(
                                Menu.NONE,
                                ID_SHARE,
                                2,
                                context.getString(R.string.action_share)
                            )
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                            return true
                        }

                        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                            if (menu.findItem(ID_HIGHLIGHT) == null)
                                menu.add(
                                    Menu.NONE,
                                    ID_HIGHLIGHT,
                                    0,
                                    context.getString(R.string.action_highlight)
                                )
                                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                            if (menu.findItem(ID_ADD_NOTE) == null)
                                menu.add(
                                    Menu.NONE,
                                    ID_ADD_NOTE,
                                    1,
                                    context.getString(R.string.action_add_note)
                                )
                                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                            if (menu.findItem(ID_SHARE) == null)
                                menu.add(
                                    Menu.NONE,
                                    ID_SHARE,
                                    2,
                                    context.getString(R.string.action_share)
                                )
                                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                            return true
                        }

                        override fun onActionItemClicked(
                            mode: ActionMode,
                            item: MenuItem
                        ): Boolean {
                            if (item.itemId !in setOf(
                                    ID_HIGHLIGHT,
                                    ID_ADD_NOTE,
                                    ID_SHARE
                                )
                            ) return false
                            val webView = findWebView(fragmentActivity.window.decorView)
                            webView?.evaluateJavascript("window.getSelection().toString()") { raw ->
                                // Use JSONTokener to correctly unescape the JS result so that
                                // embedded \n sequences become real newlines instead of literals.
                                val text = raw?.let {
                                    runCatching {
                                        org.json.JSONTokener(it).nextValue()?.toString()
                                    }.getOrNull() ?: it.removeSurrounding("\"")
                                }?.takeIf { it.isNotBlank() }
                                when (item.itemId) {
                                    ID_HIGHLIGHT -> onRequestAnnotation(
                                        AnnotationRequest.Highlight(
                                            text
                                        )
                                    )

                                    ID_ADD_NOTE -> onRequestAnnotation(AnnotationRequest.Note(text))
                                    ID_SHARE -> if (text != null) {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    }
                                }
                                mode.finish()
                            } ?: mode.finish()
                            return true
                        }

                        override fun onDestroyActionMode(mode: ActionMode) {}
                    }

                    val initialTheme = findReaderTheme(viewModel.themeId.value)
                    val initialColCount = when (viewModel.columnCount.value) {
                        "one" -> ColumnCount.ONE
                        "two" -> ColumnCount.TWO
                        else -> ColumnCount.AUTO
                    }
                    val initialPrefs = EpubPreferences(
                        backgroundColor = ReadiumColor(initialTheme.background.toArgb()),
                        textColor = ReadiumColor(initialTheme.textColor.toArgb()),
                        publisherStyles = false,
                        fontSize = viewModel.fontSize.value,
                        scroll = viewModel.pageScroll.value,
                        columnCount = initialColCount,
                        textAlign = if (viewModel.justifyText.value) TextAlign.JUSTIFY else null,
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

                    // Bookmark
                    Row {
                        IconButton(
                            onClick = {
                                if (isCurrentPageBookmarked) {
                                    val match = bookmarks.find { bm ->
                                        runCatching {
                                            val bmLoc = Locator.fromJSON(JSONObject(bm.locator))
                                                ?: return@runCatching false
                                            bmLoc.href == currentLocator?.href &&
                                                    abs(
                                                        (bmLoc.locations.progression ?: 0.0) -
                                                                (currentLocator?.locations?.progression
                                                                    ?: 0.0)
                                                    ) < 0.03
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

        // ── Inline Highlight Color Picker ─────────────────────────────────────
        if (showHighlightColorPicker) {
            val pendingHighlightText by viewModel.pendingSelectedText.collectAsState()
            HighlightColorPicker(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                onColorSelected = { colorArgb ->
                    coroutineScope.launch {
                        val selectable = navigator as? SelectableNavigator
                        val locator = selectable?.currentSelection()?.locator
                            ?: navigator?.currentLocator?.value
                            ?: run { viewModel.dismissHighlightColorPicker(); return@launch }
                        onSaveAnnotation(
                            AnnotationEntity(
                                bookId = bookId,
                                type = "highlight",
                                color = colorArgb,
                                locator = locator.toJSON().toString(),
                                text = pendingHighlightText,
                                note = null,
                            )
                        )
                        selectable?.clearSelection()
                        viewModel.dismissHighlightColorPicker()
                    }
                },
                onDismiss = {
                    coroutineScope.launch {
                        (navigator as? SelectableNavigator)?.clearSelection()
                        viewModel.dismissHighlightColorPicker()
                    }
                },
            )
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
                            icon = {
                                Icon(
                                    PhosphorIcons.Regular.List,
                                    contentDescription = null,
                                    tint = chromeIcon
                                )
                            },
                            label = "Contents",
                            labelColor = chromeLabel,
                            onClick = { showContents = true },
                        )
                        ReaderNavItem(
                            icon = {
                                Icon(
                                    PhosphorIcons.Regular.MagnifyingGlass,
                                    contentDescription = null,
                                    tint = chromeIcon
                                )
                            },
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
                        ReaderNavItem(
                            icon = {
                                Icon(
                                    PhosphorIcons.Regular.TextAa,
                                    contentDescription = null,
                                    tint = chromeIcon
                                )
                            },
                            label = "Themes",
                            labelColor = chromeLabel,
                            onClick = { showThemes = true },
                        )
                    }
                }
            }
        }
    }

    // ── Table of Contents ─────────────────────────────────────────────────────
    if (showContents) {
        com.example.liber.ui.components.LiberModalBottomSheet(
            onDismissRequest = { showContents = false },
            title = "Contents",
        ) {
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
        com.example.liber.ui.components.LiberModalBottomSheet(
            onDismissRequest = { showSearch = false },
            title = "Search in Book",
        ) {
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
        com.example.liber.ui.components.LiberModalBottomSheet(
            onDismissRequest = { showNotebook = false },
            title = "Notebook",
        ) {
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
        com.example.liber.ui.components.LiberModalBottomSheet(
            onDismissRequest = { showThemes = false },
            title = "Themes & Settings",
        ) {
            ThemesSheet(
                currentThemeId = themeId,
                onThemeChange = { id -> viewModel.setTheme(id) },
                onDecreaseFontSize = { viewModel.adjustFontSize(-0.1) },
                onIncreaseFontSize = { viewModel.adjustFontSize(+0.1) },
                pageScroll = pageScroll,
                onPageScrollChange = { viewModel.setPageScroll(it) },
                customizeLayout = customizeLayout,
                onCustomizeLayoutChange = { viewModel.setCustomizeLayout(it) },
                lineSpacing = lineSpacing,
                onLineSpacingChange = { viewModel.setLineSpacing(it) },
                characterSpacing = characterSpacing,
                onCharacterSpacingChange = { viewModel.setCharacterSpacing(it) },
                wordSpacing = wordSpacing,
                onWordSpacingChange = { viewModel.setWordSpacing(it) },
                margins = margins,
                onMarginsChange = { viewModel.setMargins(it) },
                columnCount = columnCount,
                onColumnCountChange = { viewModel.setColumnCount(it) },
                justifyText = justifyText,
                onJustifyTextChange = { viewModel.setJustifyText(it) },
                onResetSettings = { viewModel.resetLayoutSettings() },
                onClose = { showThemes = false },
            )
        }
    }

    // ── Create / Edit Annotation ──────────────────────────────────────────────
    if (showAnnotationCreator) {
        val noteText by viewModel.annotationNoteText.collectAsState()
        val selectedColor by viewModel.annotationColorArgb.collectAsState()
        val selectedText by viewModel.pendingSelectedText.collectAsState()
        val annotationType by viewModel.pendingAnnotationType.collectAsState()
        val title = if (annotationType == "highlight") "Highlight" else "New Note"

        com.example.liber.ui.components.LiberModalBottomSheet(
            onDismissRequest = { viewModel.cancelAnnotation() },
            title = title,
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
                        val editingId = viewModel.editingAnnotationId.value
                        if (editingId != null) {
                            // Editing an existing annotation — REPLACE via same primary key
                            val existing = annotations.find { it.id == editingId }
                            if (existing != null) {
                                val updatedNote = noteText.ifBlank { null }
                                onSaveAnnotation(
                                    existing.copy(
                                        type = if (updatedNote != null) "note" else "highlight",
                                        note = updatedNote,
                                        color = selectedColor,
                                    )
                                )
                            }
                        } else {
                            // Brand-new annotation from the current text selection
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
                        }
                        viewModel.cancelAnnotation()
                    }
                },
                onCancel = { viewModel.cancelAnnotation() },
            )
        }
    }

    // ── Annotation Action Sheet (tap on existing highlight) ───────────────────
    LocalContext.current
    if (tappedAnnotation != null) {
        val sheetTitle = if (tappedAnnotation.type == "highlight") "Highlight" else "Note"
        com.example.liber.ui.components.LiberModalBottomSheet(
            onDismissRequest = { viewModel.dismissAnnotationMenu() },
            title = sheetTitle,
        ) {
            AnnotationActionsSheet(
                annotation = tappedAnnotation,
                onEditNote = { viewModel.startAnnotationEdit(tappedAnnotation) },
                onShare = {
                    val shareText = tappedAnnotation.text
                    if (!shareText.isNullOrBlank()) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        fragmentActivity.startActivity(Intent.createChooser(intent, null))
                    }
                    viewModel.dismissAnnotationMenu()
                },
                onDelete = {
                    onDeleteAnnotation(tappedAnnotation.id)
                    viewModel.dismissAnnotationMenu()
                },
                onDismiss = { viewModel.dismissAnnotationMenu() },
            )
        }
    }
}

// ── Progress Scrubber ─────────────────────────────────────────────────────────

@Composable
private fun ProgressScrubber(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val progressColor = MaterialTheme.colorScheme.onSurfaceVariant

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
internal fun ReaderNavItem(
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
            color = if (depth == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )

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
            placeholder = {
                Text(
                    "Search in book...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    PhosphorIcons.Regular.MagnifyingGlass,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.search("") }) {
                        Text(
                            "✕",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        )

        if (isSearching && searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (searchResults.isEmpty() && searchQuery.isNotBlank() && !isSearching) {
            EmptyState(
                title = "No results found",
                subtitle = "Try a different search term.",
                image = R.drawable.want_to_read_empty,
                showImage = false,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
                subtitleColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 20.dp)
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
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Text(
            text = buildAnnotatedString {
                append(locator.text.before ?: "")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Unspecified)) {
                    append(locator.text.highlight ?: "")
                }
                append(locator.text.after ?: "")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(horizontal = 20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
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
    val tabs = listOf(
        "bookmarks" to "Bookmarks",
        "highlights" to "Highlights",
        "notes" to "Notes"
    )
    var activeTab by remember { mutableStateOf("bookmarks") }

    Column(modifier = modifier.fillMaxWidth()) {
        // Filter tabs (Pill style to match React design)
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { (id, label) ->
                    val selected = activeTab == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.surfaceContainerHighest
                                else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { activeTab = id },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
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
                    val highlights =
                        remember(annotations) { annotations.filter { it.type == "highlight" } }
                    AnnotationList(
                        annotations = highlights,
                        emptyMessage = "No highlights yet.",
                        onNoteClick = onNoteClick,
                        onDeleteNote = onDeleteNote,
                        emptyImage = R.drawable.highlights_empty,
                    )
                }

                "notes" -> {
                    val notes = remember(annotations) { annotations.filter { it.type == "note" } }
                    AnnotationList(
                        annotations = notes,
                        emptyMessage = "No notes yet.",
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
            title = "No bookmarks.",
            image = R.drawable.bookmarks_empty,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
            subtitleColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            modifier = modifier.padding(horizontal = 20.dp),
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
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            PhosphorIcons.Regular.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        IconButton(
                            onClick = { onDeleteBookmark(bm) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                PhosphorIcons.Regular.Trash,
                                contentDescription = "Delete bookmark",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .padding(horizontal = 20.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
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
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
            subtitleColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            modifier = modifier.padding(horizontal = 20.dp),
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
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
internal fun DarkAnnotationItem(
    annotation: AnnotationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateStr = remember(annotation.createdAt) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(annotation.createdAt))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        // Header Row: Chapter & Date + Trash Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chapter • $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    PhosphorIcons.Regular.Trash,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Content Row: Color Border + (Quote & Note)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Accent Border (Highlight Color)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        Color(annotation.color).copy(alpha = 1f),
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (!annotation.text.isNullOrBlank()) {
                    Text(
                        text = "\"${annotation.text}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (!annotation.note.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            PhosphorIcons.Regular.NotePencil,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 2.dp),
                        )
                        Text(
                            text = annotation.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
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
    pageScroll: Boolean,
    onPageScrollChange: (Boolean) -> Unit,
    customizeLayout: Boolean,
    onCustomizeLayoutChange: (Boolean) -> Unit,
    lineSpacing: Double,
    onLineSpacingChange: (Double) -> Unit,
    characterSpacing: Double,
    onCharacterSpacingChange: (Double) -> Unit,
    wordSpacing: Double,
    onWordSpacingChange: (Double) -> Unit,
    margins: Double,
    onMarginsChange: (Double) -> Unit,
    columnCount: String,
    onColumnCountChange: (String) -> Unit,
    justifyText: Boolean,
    onJustifyTextChange: (Boolean) -> Unit,
    onResetSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Page Flipping segment ─────────────────────────────────────────────
        Text(
            text = "PAGE FLIPPING",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp)),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (!pageScroll) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else Color.Transparent,
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
                    .clickable { onPageScrollChange(false) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Default",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (!pageScroll) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (!pageScroll) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .align(Alignment.CenterVertically)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (pageScroll) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else Color.Transparent,
                        RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                    )
                    .clickable { onPageScrollChange(true) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Vertical",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (pageScroll) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (pageScroll) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Font size row ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp)),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onDecreaseFontSize)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    PhosphorIcons.Regular.Minus, contentDescription = "Decrease font",
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .align(Alignment.CenterVertically)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onIncreaseFontSize)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    PhosphorIcons.Regular.TextAa, contentDescription = "Increase font",
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Theme grid (3 columns × 2 rows) ───────────────────────────────────
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
                repeat(3 - rowThemes.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Accessibility & Layout Options ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Accessibility & Layout Options",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))

        // Customize toggle row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Customize",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(
                checked = customizeLayout,
                onCheckedChange = onCustomizeLayoutChange,
            )
        }

        // Animated sliders section
        AnimatedVisibility(
            visible = customizeLayout,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // LINE SPACING
                LayoutSliderRow(
                    label = "LINE SPACING",
                    icon = {
                        Icon(
                            PhosphorIcons.Regular.LineSegments,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    value = lineSpacing.toFloat(),
                    valueText = "${"%,.2f".format(lineSpacing)}",
                    valueRange = 0.8f..2.5f,
                    onValueChange = { onLineSpacingChange(it.toDouble()) },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                // CHARACTER SPACING
                LayoutSliderRow(
                    label = "CHARACTER SPACING",
                    icon = {
                        Text(
                            "Abc",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    value = characterSpacing.toFloat(),
                    valueText = if (characterSpacing == 0.0) "0%" else "${characterSpacing.toInt()}%",
                    valueRange = -10f..10f,
                    onValueChange = { onCharacterSpacingChange(it.toDouble()) },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                // WORD SPACING
                LayoutSliderRow(
                    label = "WORD SPACING",
                    icon = {
                        Icon(
                            PhosphorIcons.Regular.Paragraph,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    value = wordSpacing.toFloat(),
                    valueText = if (wordSpacing == 0.0) "0%" else "${wordSpacing.toInt()}%",
                    valueRange = -20f..20f,
                    onValueChange = { onWordSpacingChange(it.toDouble()) },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                // MARGINS
                LayoutSliderRow(
                    label = "MARGINS",
                    icon = {
                        Icon(
                            PhosphorIcons.Regular.FrameCorners,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    value = margins.toFloat(),
                    valueText = if (margins == 0.0) "0%" else "${margins.toInt()}%",
                    valueRange = -10f..10f,
                    onValueChange = { onMarginsChange(it.toDouble()) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Columns dropdown ──────────────────────────────────────────────────
        var showColumnsDropdown by remember { mutableStateOf(false) }
        val columnsLabel = when (columnCount) {
            "one" -> "1"
            "two" -> "2"
            else -> "Auto Set"
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Columns",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box {
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { showColumnsDropdown = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        columnsLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        PhosphorIcons.Regular.CaretDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = showColumnsDropdown,
                    onDismissRequest = { showColumnsDropdown = false },
                ) {
                    listOf(
                        "auto" to "Auto Set",
                        "one" to "1",
                        "two" to "2"
                    ).forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onColumnCountChange(key); showColumnsDropdown = false },
                            trailingIcon = if (columnCount == key) {
                                {
                                    Icon(
                                        PhosphorIcons.Regular.Check,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Justify Text ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Justify Text",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(
                checked = justifyText,
                onCheckedChange = onJustifyTextChange,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Reset Theme button ────────────────────────────────────────────────
        Button(
            onClick = onResetSettings,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                "Reset Theme",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
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
                    Modifier.border(
                        3.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp)
                    )
                else
                    Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
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

// ── AnnotationActionsSheet ────────────────────────────────────────────────────

@Composable
private fun AnnotationActionsSheet(
    annotation: AnnotationEntity,
    onEditNote: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val highlightColor = Color(annotation.color.toLong() and 0xFFFFFFFFL).copy(alpha = 1f)
    val hasNote = !annotation.note.isNullOrBlank()

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))

        // Quoted text preview
        if (!annotation.text.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(highlightColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "\"${annotation.text}\"",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = Color.Black.copy(alpha = 0.82f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Existing note preview
        if (hasNote) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    PhosphorIcons.Regular.NotePencil,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp),
                )
                Text(
                    text = annotation.note!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Action rows
        AnnotationActionRow(
            icon = PhosphorIcons.Regular.NotePencil,
            label = if (hasNote) "Edit note" else "Add note",
            onClick = onEditNote,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .padding(start = 56.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )

        AnnotationActionRow(
            icon = PhosphorIcons.Regular.Export,
            label = "Share",
            onClick = onShare,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .padding(start = 56.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )

        AnnotationActionRow(
            icon = PhosphorIcons.Regular.Trash,
            label = "Remove highlight",
            tint = MaterialTheme.colorScheme.error,
            onClick = onDelete,
        )

        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun AnnotationActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
        )
    }
}

// ── HighlightColorPicker ──────────────────────────────────────────────────────

@Composable
private fun HighlightColorPicker(
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = Color(0xCC1C1C1E),
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnnotationColorOptions.forEach { colorArgb ->
                // Display circles at full opacity so the hue is clearly visible in the picker.
                val hue = Color(colorArgb.toLong() and 0xFFFFFFFFL).copy(alpha = 1f)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(hue, CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable { onColorSelected(colorArgb) }
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "✕",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
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
    if (isHighlight) "Highlight" else "New Note"
    val saveLabel = if (isHighlight) "Save highlight" else "Save note"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        currentChapter?.let { chapter ->
            Text(
                text = chapter,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        )
                        .clickable { onColorChange(colorArgb) }
                )
            }
        }

        OutlinedTextField(
            value = noteText,
            onValueChange = onNoteTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = {
                Text(
                    if (isHighlight) "Add a note… (optional)" else "Write your note…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                ),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(saveLabel)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Layout slider row ─────────────────────────────────────────────────────────

@Composable
private fun LayoutSliderRow(
    label: String,
    icon: @Composable () -> Unit,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            icon()
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}


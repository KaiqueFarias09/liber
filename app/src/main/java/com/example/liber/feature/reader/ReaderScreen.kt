package com.example.liber.feature.reader

import android.app.Activity
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
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
import com.adamglin.phosphoricons.regular.List
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.NotePencil
import com.adamglin.phosphoricons.regular.TextAa
import com.example.liber.R
import com.example.liber.data.model.AnnotationEntity
import com.example.liber.data.model.BookmarkEntity
import com.example.liber.data.repository.UserPreferencesRepository
import com.example.liber.feature.reader.components.AnnotationActionsSheet
import com.example.liber.feature.reader.components.CreateAnnotationSheet
import com.example.liber.feature.reader.components.HighlightColorPicker
import com.example.liber.feature.reader.components.NotebookView
import com.example.liber.feature.reader.components.SearchView
import com.example.liber.feature.reader.components.ThemesSheet
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
import java.util.Locale
import kotlin.math.abs
import org.readium.r2.navigator.preferences.Color as ReadiumColor

private const val ID_HIGHLIGHT = 9_001
private const val ID_ADD_NOTE = 9_002
private const val ID_SHARE = 9_003

// Design constants
internal val GreenAccent = Color(0xFF32D74B)
internal val RedAccent = Color(0xFFFF3B30)

/** Recursively finds all [WebView]s in the view hierarchy. */
private fun findAllWebViews(view: View?): List<WebView> {
    if (view == null) return emptyList()
    if (view is WebView) return listOf(view)

    val webViews = mutableListOf<WebView>()
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            webViews.addAll(findAllWebViews(view.getChildAt(i)))
        }
    }
    return webViews
}


private fun formatShareText(text: String, publication: Publication): String {
    val title = publication.metadata.title
    val author = publication.metadata.authors.firstOrNull()?.name
    val publisher = publication.metadata.publishers.firstOrNull()?.name
    val publishedDate = publication.metadata.published

    return buildString {
        append("“").append(text).append("”")
        append("\n\n")
        append("Excerpt From: ")
        if (author != null) {
            append(author).append(". ")
        }
        append("“").append(title ?: "Unknown Title").append(".”")

        val metadataParts = mutableListOf<String>()
        if (publisher != null) metadataParts.add(publisher)
        if (publishedDate != null) {
            val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
            metadataParts.add(yearFormat.format(publishedDate.toJavaDate()))
        }

        if (metadataParts.isNotEmpty()) {
            append(" ")
            append(metadataParts.joinToString(", "))
            append(".")
        }

        append(" Liber.")
        append("\n\n")
        append("This material may be protected by copyright.")
    }
}

/**
 * Full-screen EPUB reader with a design matching the app's iOS-inspired aesthetic.
 * Features: theme switching (persisted), bookmarks, annotations, search, table of contents.
 */
@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    publication: Publication,
    bookId: String,
    userPreferencesRepository: UserPreferencesRepository,
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
        factory = ReaderViewModel.Factory(application, publication, userPreferencesRepository)
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

    // Ensure status bar icons have enough contrast against the reader theme.
    // SideEffect handles theme changes within the reader, while DisposableEffect
    // captures and restores the app's global status bar state on exit.
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        val insets = WindowCompat.getInsetsController(window, view)
        insets.isAppearanceLightStatusBars = !theme.isDark
        insets.isAppearanceLightNavigationBars = !theme.isDark
    }
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val insets = WindowCompat.getInsetsController(window, view)
        val originalStatus = insets.isAppearanceLightStatusBars
        val originalNav = insets.isAppearanceLightNavigationBars
        onDispose {
            insets.isAppearanceLightStatusBars = originalStatus
            insets.isAppearanceLightNavigationBars = originalNav
        }
    }

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
            viewModel.startHighlightColorPicker(
                pendingAnnotationRequest.selectedText,
                pendingAnnotationRequest.locator
            )
        } else {
            viewModel.startAnnotation(
                type = "note",
                prefilledText = pendingAnnotationRequest.selectedText,
                locator = pendingAnnotationRequest.locator
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
        val webViews = findAllWebViews(fragmentActivity.window.decorView)
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
        webViews.forEach { it.evaluateJavascript(js, null) }
    }

    // Continuous vertical scroll: when scroll mode is on, inject a JS listener into Readium's
    // iframe that detects scroll-to-end and calls goForward() automatically, so the user can
    // scroll all the way from chapter 1 to the last chapter without any manual gesture.
    LaunchedEffect(currentLocator?.href, pageScroll, navigator) {
        if (!pageScroll) return@LaunchedEffect
        val epubFrag = navigator as? EpubNavigatorFragment ?: return@LaunchedEffect
        // Wait for the page to finish loading after a chapter transition.
        delay(400)
        val webViews = findAllWebViews(fragmentActivity.window.decorView)

        @Suppress("StringShouldBeRawString")
        val js = """
            (function setupContinuousScroll() {
                var THRESHOLD = 8; // px from bottom that counts as "end"
                var advancing = false;

                function atEnd(scrollable) {
                    return (scrollable.scrollTop + scrollable.clientHeight) >= (scrollable.scrollHeight - THRESHOLD);
                }

                // Try the top-level window first (single-doc mode), then each iframe.
                function attachListener(win) {
                    if (!win || !win.document) return;
                    var root = win.document.scrollingElement || win.document.documentElement;
                    if (!root) return;
                    win.addEventListener('scroll', function() {
                        if (advancing) return;
                        if (atEnd(root)) {
                            advancing = true;
                            // Small debounce so a momentary glitch doesn't double-fire.
                            setTimeout(function() { advancing = false; }, 1000);
                            window._liberGoForward && window._liberGoForward();
                        }
                    }, { passive: true });
                }

                // The outer shell
                attachListener(window);

                // All inner iframes (Readium per-spine-item webviews)
                var iframes = document.querySelectorAll('iframe');
                for (var i = 0; i < iframes.length; i++) {
                    try { attachListener(iframes[i].contentWindow); } catch(e) {}
                }
            })();
        """.trimIndent()

        // Register the native callback and script on all webviews
        webViews.forEach { wv ->
            wv.evaluateJavascript(js, null)
            wv.post {
                wv.addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun invoke() {
                        wv.post {
                            epubFrag.goForward(animated = false)
                        }
                    }
                }, "_liberGoForwardBridge")

                wv.evaluateJavascript(
                    "window._liberGoForward = function() { _liberGoForwardBridge.invoke(); };",
                    null
                )
            }
        }
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

                            coroutineScope.launch {
                                val selectable = navigator as? SelectableNavigator
                                val selection = selectable?.currentSelection()
                                val locator = selection?.locator

                                val wvs = findAllWebViews(fragmentActivity.window.decorView)
                                var textFromJs: String? = null

                                if (wvs.isNotEmpty()) {
                                    for (wv in wvs) {
                                        val t =
                                            kotlin.coroutines.suspendCoroutine<String?> { cont ->
                                                val js = """
                                                    (function() {
                                                        var sel = window.getSelection().toString();
                                                        if (sel) return sel;
                                                        var iframes = document.querySelectorAll('iframe');
                                                        for (var i = 0; i < iframes.length; i++) {
                                                            try {
                                                                sel = iframes[i].contentWindow.getSelection().toString();
                                                                if (sel) return sel;
                                                            } catch (e) {}
                                                        }
                                                        return "";
                                                    })()
                                                """.trimIndent()
                                                wv.evaluateJavascript(js) { raw ->
                                                    val unescaped = raw?.let {
                                                        runCatching {
                                                            org.json.JSONTokener(it)
                                                                .nextValue()?.toString()
                                                        }.getOrNull()
                                                            ?: it.removeSurrounding("\"")
                                                    }?.takeIf { it.isNotBlank() }
                                                    cont.resumeWith(Result.success(unescaped))
                                                }
                                            }
                                        if (t != null) {
                                            textFromJs = t
                                            break
                                        }
                                    }
                                }

                                val finalText = textFromJs ?: locator?.text?.highlight

                                when (item.itemId) {
                                    ID_HIGHLIGHT -> onRequestAnnotation(
                                        AnnotationRequest.Highlight(finalText, locator)
                                    )

                                    ID_ADD_NOTE -> onRequestAnnotation(
                                        AnnotationRequest.Note(finalText, locator)
                                    )

                                    ID_SHARE -> if (finalText != null) {
                                        val shareText =
                                            formatShareText(finalText, publication)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                intent,
                                                null
                                            )
                                        )
                                    }
                                }
                                mode.finish()
                            }
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
            val pendingLocator by viewModel.pendingLocator.collectAsState()

            HighlightColorPicker(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                onColorSelected = { colorArgb ->
                    coroutineScope.launch {
                        val selectable = navigator as? SelectableNavigator
                        val locator = pendingLocator
                            ?: selectable?.currentSelection()?.locator
                            ?: navigator?.currentLocator?.value
                            ?: run { viewModel.dismissHighlightColorPicker(); return@launch }

                        val textToSave = pendingHighlightText
                            ?: locator.text.highlight

                        onSaveAnnotation(
                            AnnotationEntity(
                                bookId = bookId,
                                type = "highlight",
                                color = colorArgb,
                                locator = locator.toJSON().toString(),
                                text = textToSave,
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
        com.example.liber.core.designsystem.LiberModalBottomSheet(
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
        com.example.liber.core.designsystem.LiberModalBottomSheet(
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
        com.example.liber.core.designsystem.LiberModalBottomSheet(
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
        com.example.liber.core.designsystem.LiberModalBottomSheet(
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

        com.example.liber.core.designsystem.LiberModalBottomSheet(
            onDismissRequest = { viewModel.cancelAnnotation() },
            title = title,
        ) {
            CreateAnnotationSheet(
                annotationType = annotationType,
                selectedText = selectedText,
                noteText = noteText,
                selectedColorArgb = selectedColor,
                currentChapter = currentLocator?.title
                    ?: currentLocator?.href?.toString(),
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
                            val pendingLocator = viewModel.pendingLocator.value
                            val selectable = navigator as? SelectableNavigator
                            val locator = pendingLocator
                                ?: selectable?.currentSelection()?.locator
                                ?: navigator?.currentLocator?.value
                                ?: return@launch

                            val textToSave = selectedText
                                ?: locator.text.highlight

                            onSaveAnnotation(
                                AnnotationEntity(
                                    bookId = bookId,
                                    type = annotationType,
                                    color = selectedColor,
                                    locator = locator.toJSON().toString(),
                                    text = textToSave,
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
        com.example.liber.core.designsystem.LiberModalBottomSheet(
            onDismissRequest = { viewModel.dismissAnnotationMenu() },
            title = sheetTitle,
        ) {
            AnnotationActionsSheet(
                annotation = tappedAnnotation,
                onEditNote = { viewModel.startAnnotationEdit(tappedAnnotation) },
                onShare = {
                    val text = tappedAnnotation.text
                    if (!text.isNullOrBlank()) {
                        val shareText = formatShareText(text, publication)
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


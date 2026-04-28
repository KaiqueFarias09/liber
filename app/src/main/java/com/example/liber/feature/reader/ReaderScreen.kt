package com.example.liber.feature.reader

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Bookmark
import com.adamglin.phosphoricons.regular.List
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.NotePencil
import com.adamglin.phosphoricons.regular.ShareNetwork
import com.adamglin.phosphoricons.regular.TextAa
import com.example.liber.R
import com.example.liber.core.logging.AndroidAppLogger
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.AnnotationType
import com.example.liber.data.repository.UserPreferencesRepository
import com.example.liber.feature.dictionary.DictionaryLookupSheet
import com.example.liber.feature.dictionary.DictionaryViewModel
import com.example.liber.feature.reader.components.AnnotationActionsSheet
import com.example.liber.feature.reader.components.CreateAnnotationSheet
import com.example.liber.feature.reader.components.HighlightColorPicker
import com.example.liber.feature.reader.components.NotebookView
import com.example.liber.feature.reader.components.SearchView
import com.example.liber.feature.reader.components.SelectionActionsMenu
import com.example.liber.feature.reader.components.ThemesSheet
import kotlinx.coroutines.launch
import org.coolreader.crengine.TOCItem
import kotlin.math.abs
import com.example.liber.data.model.Bookmark as BookmarkModel

// Design constants (also used by other files in this package)
internal val GreenAccent = Color(0xFF32D74B)
internal val RedAccent = Color(0xFFFF3B30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookUri: Uri,
    bookTitle: String,
    bookId: String,
    bookLanguage: String?,
    dictionaryViewModel: DictionaryViewModel,
    userPreferencesRepository: UserPreferencesRepository,
    initialXPointer: String?,
    annotations: List<Annotation>,
    bookmarks: List<BookmarkModel>,
    pendingAnnotationRequest: AnnotationRequest?,
    onRequestAnnotation: (AnnotationRequest) -> Unit,
    onSaveLocator: (xpointer: String, progress: Int) -> Unit,
    onSaveAnnotation: (Annotation) -> Unit,
    onDeleteAnnotation: (Long) -> Unit,
    onSaveBookmark: (BookmarkModel) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onClearPendingAnnotation: () -> Unit,
    onBack: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as Application
    val appLogger = remember(application) { AndroidAppLogger(application) }
    val viewModel: ReaderViewModel = viewModel(
        key = bookId,
        factory = ReaderViewModel.Factory(
            application,
            bookUri,
            bookTitle,
            bookId,
            userPreferencesRepository,
            appLogger,
        )
    )
    val context = LocalContext.current
    val localeLanguageTag = remember(bookLanguage) {
        if (!bookLanguage.isNullOrBlank()) {
            bookLanguage
        } else {
            runCatching { context.resources.configuration.locales[0].toLanguageTag() }
                .getOrDefault("en")
        }
    }
    val density = LocalDensity.current

    // ── Observe ViewModel state ──────────────────────────────────────────────
    val showUI by viewModel.showUI.collectAsState()
    val themeId by viewModel.themeId.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val pageBitmap by viewModel.pageBitmap.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val positionProps by viewModel.positionProps.collectAsState()
    val tocItems by viewModel.tocItems.collectAsState()
    val showAnnotationCreator by viewModel.showAnnotationCreator.collectAsState()
    val showHighlightColorPicker by viewModel.showHighlightColorPicker.collectAsState()
    val tappedAnnotationId by viewModel.tappedAnnotationId.collectAsState()
    val tappedAnnotation = remember(tappedAnnotationId, annotations) {
        tappedAnnotationId?.let { id -> annotations.find { it.id == id } }
    }
    val pageScroll by viewModel.pageScroll.collectAsState()
    val customizeLayout by viewModel.customizeLayout.collectAsState()
    val lineSpacing by viewModel.lineSpacing.collectAsState()
    val characterSpacing by viewModel.characterSpacing.collectAsState()
    val wordSpacing by viewModel.wordSpacing.collectAsState()
    val margins by viewModel.margins.collectAsState()
    val selectionActive by viewModel.selectionActive.collectAsState()
    val showSelectionMenu by viewModel.showSelectionMenu.collectAsState()
    val highlightRects by viewModel.highlightRects.collectAsState()
    val selectionStartAnchor by viewModel.selectionStartAnchor.collectAsState()
    val selectionEndAnchor by viewModel.selectionEndAnchor.collectAsState()
    val pendingText by viewModel.pendingSelectedText.collectAsState()
    val fullscreenImage by viewModel.fullscreenImage.collectAsState()
    val dictionaryLookupState by dictionaryViewModel.lookupState.collectAsState()
    val activeLookupQuery by dictionaryViewModel.activeLookupQuery.collectAsState()
    var showDictionaryLookupSheet by remember { mutableStateOf(false) }

    val theme = findReaderTheme(themeId)

    // ── Status bar appearance ────────────────────────────────────────────────
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).let {
            it.isAppearanceLightStatusBars = !theme.isDark
            it.isAppearanceLightNavigationBars = !theme.isDark
        }
    }
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val insets = WindowCompat.getInsetsController(window, view)
        val origStatus = insets.isAppearanceLightStatusBars
        val origNav = insets.isAppearanceLightNavigationBars
        onDispose {
            insets.isAppearanceLightStatusBars = origStatus
            insets.isAppearanceLightNavigationBars = origNav
        }
    }

    // ── Effects ──────────────────────────────────────────────────────────────

    // Open the document when the screen appears or the book changes
    LaunchedEffect(bookId) {
        viewModel.openBook(context, initialXPointer)
    }

    // Push annotation highlights to the engine whenever the list changes
    LaunchedEffect(annotations) {
        viewModel.applyHighlights(annotations)
    }

    // Bridge a pending annotation request from outside into ViewModel state
    LaunchedEffect(pendingAnnotationRequest) {
        pendingAnnotationRequest ?: return@LaunchedEffect
        if (pendingAnnotationRequest is AnnotationRequest.Highlight) {
            viewModel.startHighlightColorPicker(
                pendingAnnotationRequest.selectedText,
                pendingAnnotationRequest.xpointer,
            )
        } else {
            viewModel.startAnnotation(
                type = "note",
                prefilledText = pendingAnnotationRequest.selectedText,
                xpointer = pendingAnnotationRequest.xpointer,
            )
        }
        onClearPendingAnnotation()
    }

    // ── Modal / panel state ──────────────────────────────────────────────────
    var showContents by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showNotebook by remember { mutableStateOf(false) }
    var showThemes by remember { mutableStateOf(false) }

    val isAnyModalOpen =
        showContents || showSearch || showNotebook || showThemes ||
                showAnnotationCreator || showHighlightColorPicker || showSelectionMenu ||
                (tappedAnnotationId != null) || (fullscreenImage != null)

    // ── Derived state ────────────────────────────────────────────────────────

    // Current chapter title from TOC (last item whose page <= current page)
    val currentChapter = remember(positionProps, tocItems) {
        val page = positionProps?.pageNumber ?: 0
        tocItems.lastOrNull { it.mPage <= page }?.mName
    }

    // Bookmarks store progress as a float string ("0.35"). In continuous
    // scroll, a jumped-to bookmark may be visible without matching the
    // viewport's exact top progress, so match against the visible Y range.
    val currentBookmark = remember(progress, positionProps, bookmarks) {
        val props = positionProps
        if (props != null && props.fullHeight > props.pageHeight) {
            val maxScrollableY = props.fullHeight - props.pageHeight
            val viewportStart = props.y
            val viewportEnd = props.y + props.pageHeight
            val tolerancePx = (props.pageHeight * 0.02f).toInt().coerceAtLeast(24)

            bookmarks
                .mapNotNull { bm ->
                    val bookmarkProgress = bm.locator.toFloatOrNull() ?: return@mapNotNull null
                    val bookmarkY = (bookmarkProgress * maxScrollableY).toInt()
                    if (bookmarkY in (viewportStart - tolerancePx)..(viewportEnd + tolerancePx)) {
                        bm to abs(bookmarkY - viewportStart)
                    } else {
                        null
                    }
                }
                .minByOrNull { it.second }
                ?.first
        } else {
            bookmarks
                .mapNotNull { bm ->
                    val bookmarkProgress = bm.locator.toFloatOrNull() ?: return@mapNotNull null
                    bm to abs(bookmarkProgress - progress)
                }
                .filter { it.second < 0.03f }
                .minByOrNull { it.second }
                ?.first
        }
    }
    val isCurrentPageBookmarked = currentBookmark != null

    // ── UI chrome colors ─────────────────────────────────────────────────────
    val chromeBg = theme.background
    val chromeIcon = theme.textColor.copy(alpha = 0.7f)
    val chromeOnIcon = theme.textColor
    val chromeLabel = theme.textColor.copy(alpha = 0.5f)

    // ── Back handling ────────────────────────────────────────────────────────
    val handleBack: () -> Unit = {
        val xpointer = viewModel.currentXPointer()
        if (xpointer != null) {
            onSaveLocator(xpointer, (progress * 100).toInt())
        }
        onBack()
    }
    BackHandler(onBack = handleBack)

    // ── Root layout ──────────────────────────────────────────────────────────
    val exportScope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background),
    ) {
        // ── Bitmap rendering surface ─────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            val widthPx = with(density) { maxWidth.roundToPx() }
            val heightPx = with(density) { maxHeight.roundToPx() }

            LaunchedEffect(widthPx, heightPx) {
                viewModel.onViewReady(widthPx, heightPx)
            }

            val bmp = pageBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isAnyModalOpen, selectionActive, pageScroll) {
                            if (isAnyModalOpen) return@pointerInput
                            val swipeThreshold = 60.dp.toPx()

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startX = down.position.x
                                val startY = down.position.y
                                var lastX = startX
                                var lastY = startY
                                var navigationDone = false

                                viewModel.preloadImageAtPoint(startX.toInt(), startY.toInt())

                                if (pageScroll) {
                                    // Scroll mode: detect movement before the long-press window so
                                    // that a slow scroll doesn't accidentally trigger text selection.
                                    // • finger moves past touch slop  → scroll
                                    // • finger still for long-press timeout → text selection
                                    // • quick tap (released before timeout, no slop) → toggleUI
                                    var slopChange: androidx.compose.ui.input.pointer.PointerInputChange? =
                                        null
                                    val completedBeforeTimeout =
                                        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                            slopChange =
                                                awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                                    change.consume()
                                                }
                                        } != null

                                    if (slopChange != null) {
                                        // Movement detected → scroll
                                        viewModel.cancelImagePreload()
                                        var prevY = slopChange!!.position.y
                                        drag(slopChange!!.id) { change ->
                                            val dy = change.position.y - prevY
                                            prevY = change.position.y
                                            lastX = change.position.x
                                            lastY = change.position.y
                                            if (dy != 0f) {
                                                navigationDone = true
                                                viewModel.scrollBy(-dy.toInt())
                                            }
                                            change.consume()
                                        }
                                        if (!navigationDone) {
                                            val annotationId =
                                                viewModel.findAnnotationAtPoint(startX, startY)
                                            if (annotationId != null) {
                                                viewModel.onAnnotationTapped(annotationId)
                                            } else {
                                                viewModel.toggleUI()
                                            }
                                        }
                                    } else if (!completedBeforeTimeout) {
                                        // Finger held still past long-press timeout → text selection
                                        val imageOpened = viewModel.consumePendingImage()
                                        if (!imageOpened) {
                                            viewModel.startTextSelection(
                                                startX.toInt(),
                                                startY.toInt()
                                            )
                                            drag(down.id) { change ->
                                                lastX = change.position.x
                                                lastY = change.position.y
                                                viewModel.updateTextSelectionDrag(
                                                    lastX.toInt(),
                                                    lastY.toInt()
                                                )
                                                change.consume()
                                            }
                                            viewModel.finalizeTextSelection(
                                                lastX.toInt(),
                                                lastY.toInt()
                                            )
                                        }
                                    } else {
                                        // Quick tap → toggleUI or annotation
                                        viewModel.cancelImagePreload()
                                        val annotationId =
                                            viewModel.findAnnotationAtPoint(startX, startY)
                                        if (annotationId != null) {
                                            viewModel.onAnnotationTapped(annotationId)
                                        } else {
                                            viewModel.toggleUI()
                                        }
                                    }
                                } else {
                                    // Page mode: long-press fires after 500 ms of stillness
                                    // (cancels on movement) → text selection or image viewer.
                                    // Movement cancels → swipe for page turn or tap for nav.
                                    val longPress = awaitLongPressOrCancellation(down.id)

                                    if (longPress != null) {
                                        val imageOpened = viewModel.consumePendingImage()
                                        if (!imageOpened) {
                                            viewModel.startTextSelection(
                                                startX.toInt(),
                                                startY.toInt()
                                            )
                                            drag(longPress.id) { change ->
                                                lastX = change.position.x
                                                lastY = change.position.y
                                                viewModel.updateTextSelectionDrag(
                                                    lastX.toInt(),
                                                    lastY.toInt()
                                                )
                                                change.consume()
                                            }
                                            viewModel.finalizeTextSelection(
                                                lastX.toInt(),
                                                lastY.toInt()
                                            )
                                        }
                                    } else {
                                        // Drag cancelled the long-press wait → page swipe / tap
                                        viewModel.cancelImagePreload()
                                        drag(down.id) { change ->
                                            lastX = change.position.x
                                            lastY = change.position.y
                                            val dx = lastX - startX
                                            val dy = lastY - startY

                                            if (!navigationDone &&
                                                abs(dx) > swipeThreshold &&
                                                abs(dx) > abs(dy)
                                            ) {
                                                navigationDone = true
                                                if (dx < 0) viewModel.nextPage() else viewModel.prevPage()
                                            }
                                            change.consume()
                                        }

                                        if (!navigationDone) {
                                            val dx = abs(lastX - startX)
                                            val dy = abs(lastY - startY)
                                            if (dx < viewConfiguration.touchSlop &&
                                                dy < viewConfiguration.touchSlop
                                            ) {
                                                val annotationId =
                                                    viewModel.findAnnotationAtPoint(startX, startY)
                                                if (annotationId != null) {
                                                    viewModel.onAnnotationTapped(annotationId)
                                                } else {
                                                    val w = size.width.toFloat()
                                                    when {
                                                        startX < w * 0.3f -> viewModel.prevPage()
                                                        startX > w * 0.7f -> viewModel.nextPage()
                                                        else -> viewModel.toggleUI()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                )

                // Custom-colored highlight overlay drawn on top of the crengine bitmap.
                // Rects are in screen pixels matching the bitmap coordinate space.
                if (highlightRects.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (rect in highlightRects) {
                            drawRect(
                                color = Color(rect.color),
                                topLeft = Offset(rect.left.toFloat(), rect.top.toFloat()),
                                size = Size(
                                    (rect.right - rect.left).toFloat(),
                                    (rect.bottom - rect.top).toFloat(),
                                ),
                            )
                        }
                    }
                }

                // Draggable selection handles — shown when the selection menu is visible.
                if (showSelectionMenu) {
                    selectionStartAnchor?.let { anchor ->
                        SelectionHandle(
                            anchor = anchor,
                            isStart = true,
                            onDrag = { x, y -> viewModel.moveSelectionStartHandle(x, y) },
                            onDragEnd = { viewModel.finalizeHandleDrag() },
                        )
                    }
                    selectionEndAnchor?.let { anchor ->
                        SelectionHandle(
                            anchor = anchor,
                            isStart = false,
                            onDrag = { x, y -> viewModel.moveSelectionEndHandle(x, y) },
                            onDragEnd = { viewModel.finalizeHandleDrag() },
                        )
                    }

                    // Floating selection menu, anchored just below the selection handles.
                    // Handles are 40.dp tall positioned at anchor.y, so offset by handle height + gap.
                    val endAnchor = selectionEndAnchor
                    val handleHeightPx = with(density) { 40.dp.toPx() }
                    val menuGapPx = with(density) { 8.dp.toPx() }
                    val maxMenuY = heightPx.toFloat() - with(density) { 80.dp.toPx() }
                    val menuTopY =
                        ((endAnchor?.y ?: (heightPx * 0.5f)) + handleHeightPx + menuGapPx)
                            .coerceAtMost(maxMenuY)
                            .coerceAtLeast(0f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, menuTopY.toInt()) },
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        SelectionActionsMenu(
                            onHighlight = { viewModel.onSelectionMenuHighlight() },
                            onNote = { viewModel.onSelectionMenuNote() },
                            onDefine = {
                                val selected = pendingText?.trim().orEmpty()
                                if (selected.isNotEmpty()) {
                                    appLogger.debug(
                                        "Dictionary lookup triggered for: '$selected' (Tag: $localeLanguageTag)",
                                        tag = "ReaderScreen",
                                    )
                                    dictionaryViewModel.lookupWord(
                                        query = selected,
                                        languageTag = localeLanguageTag,
                                        sourceBookId = bookId,
                                    )
                                    showDictionaryLookupSheet = true
                                }
                                viewModel.dismissSelectionMenu()
                            },
                            onShare = {
                                if (!pendingText.isNullOrBlank()) {
                                    val shareText = buildString {
                                        append("\u201C").append(pendingText).append("\u201D\n\n")
                                        append(context.getString(R.string.reader_share_excerpt_from))
                                        append("\u201C").append(bookTitle)
                                            .append(".\u201D Liber.\n\n")
                                        append(context.getString(R.string.reader_share_copyright_notice))
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            },
                                            null,
                                        )
                                    )
                                }
                                viewModel.dismissSelectionMenu()
                            },
                            onDismiss = { viewModel.dismissSelectionMenu() },
                        )
                    }
                }
            }
        }

        // ── Top Bar ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showUI && !isAnyModalOpen,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = chromeBg,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = handleBack,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                PhosphorIcons.Regular.ArrowLeft,
                                contentDescription = "Back",
                                tint = chromeOnIcon,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                color = chromeOnIcon.copy(alpha = 0.6f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 56.dp)
                        )

                        IconButton(
                            onClick = {
                                if (isCurrentPageBookmarked) {
                                    currentBookmark?.let { onDeleteBookmark(it.id) }
                                } else {
                                    onSaveBookmark(
                                        BookmarkModel(
                                            bookId = bookId,
                                            locator = progress.toString(),
                                            chapter = currentChapter,
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                PhosphorIcons.Regular.Bookmark,
                                contentDescription = if (isCurrentPageBookmarked) "Remove bookmark" else "Add bookmark",
                                tint = if (isCurrentPageBookmarked) RedAccent else chromeIcon,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                // Gradient shadow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(chromeBg, Color.Transparent)
                            )
                        )
                )
            }
        }

        // ── Inline Highlight Color Picker ────────────────────────────────────
        if (showHighlightColorPicker) {
            val pendingXPointer by viewModel.pendingXPointer.collectAsState()
            val pendingEndXPointer by viewModel.pendingEndXPointer.collectAsState()
            HighlightColorPicker(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                onColorSelected = { colorArgb ->
                    val xptr = pendingXPointer ?: viewModel.currentXPointer()
                    ?: run { viewModel.dismissHighlightColorPicker(); return@HighlightColorPicker }
                    onSaveAnnotation(
                        Annotation(
                            bookId = bookId,
                            type = AnnotationType.HIGHLIGHT,
                            color = colorArgb,
                            locator = xptr,
                            endLocator = pendingEndXPointer ?: "",
                            text = viewModel.pendingSelectedText.value,
                        )
                    )
                    viewModel.dismissHighlightColorPicker()
                },
                onDismiss = { viewModel.dismissHighlightColorPicker() },
            )
        }

        // ── Bottom Bar ────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showUI && !isAnyModalOpen,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Gradient shadow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, chromeBg)
                            )
                        )
                )
                Surface(
                    color = chromeBg,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp, bottom = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = chromeLabel,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                            ProgressScrubber(
                                progress = progress,
                                onProgressChange = { viewModel.goToProgress(it) },
                                trackColor = theme.textColor.copy(alpha = 0.1f),
                                progressColor = RedAccent,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.labelSmall,
                                color = chromeLabel,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ReaderNavItem(
                                icon = {
                                    Icon(
                                        PhosphorIcons.Regular.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = chromeIcon
                                    )
                                },
                                label = UiText.StringResource(R.string.reader_nav_contents),
                                labelColor = chromeIcon,
                                onClick = { showContents = true }
                            )
                            ReaderNavItem(
                                icon = {
                                    Icon(
                                        PhosphorIcons.Regular.MagnifyingGlass,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = chromeIcon
                                    )
                                },
                                label = UiText.StringResource(R.string.reader_nav_search),
                                labelColor = chromeIcon,
                                onClick = { showSearch = true }
                            )
                            ReaderNavItem(
                                icon = {
                                    Box {
                                        Icon(
                                            PhosphorIcons.Regular.NotePencil,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = chromeIcon
                                        )
                                        if (bookmarks.isNotEmpty() || annotations.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 2.dp, y = (-2).dp)
                                                    .background(GreenAccent, CircleShape)
                                            )
                                        }
                                    }
                                },
                                label = UiText.StringResource(R.string.reader_nav_notebook),
                                labelColor = chromeIcon,
                                onClick = { showNotebook = true }
                            )
                            ReaderNavItem(
                                icon = {
                                    Icon(
                                        PhosphorIcons.Regular.TextAa,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = chromeIcon
                                    )
                                },
                                label = UiText.StringResource(R.string.reader_nav_themes),
                                labelColor = chromeIcon,
                                onClick = { showThemes = true }
                            )
                        }
                    }
                }
            }
        }

        // ── Search overlay (floating top bar — keeps reader content visible) ─
        AnimatedVisibility(
            visible = showSearch,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            SearchView(
                viewModel = viewModel,
                onDismiss = {
                    viewModel.clearSearch()
                    showSearch = false
                },
            )
        }

        // ── Fullscreen image viewer ──────────────────────────────────────────
        val imageScope = rememberCoroutineScope()
        AnimatedVisibility(
            visible = fullscreenImage != null,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
        ) {
            fullscreenImage?.let { bmp ->
                com.example.liber.feature.reader.components.ImageViewerOverlay(
                    bitmap = bmp,
                    onShare = {
                        imageScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val file = java.io.File(context.cacheDir, "liber_share_image.jpg")
                            file.outputStream().use {
                                bmp.compress(
                                    android.graphics.Bitmap.CompressFormat.JPEG,
                                    95,
                                    it
                                )
                            }
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "image/jpeg"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        },
                                        null,
                                    )
                                )
                            }
                        }
                    },
                    onSave = {
                        imageScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val filename = "liber_image_${System.currentTimeMillis()}.jpg"
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                val cv = android.content.ContentValues().apply {
                                    put(
                                        android.provider.MediaStore.Downloads.DISPLAY_NAME,
                                        filename
                                    )
                                    put(
                                        android.provider.MediaStore.Downloads.MIME_TYPE,
                                        "image/jpeg"
                                    )
                                    put(
                                        android.provider.MediaStore.Downloads.RELATIVE_PATH,
                                        android.os.Environment.DIRECTORY_DOWNLOADS
                                    )
                                }
                                val saveUri = context.contentResolver.insert(
                                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv
                                )
                                saveUri?.let {
                                    context.contentResolver.openOutputStream(it)?.use { out ->
                                        bmp.compress(
                                            android.graphics.Bitmap.CompressFormat.JPEG,
                                            95,
                                            out
                                        )
                                    }
                                }
                            }
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    R.string.reader_image_saved,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onDismiss = { viewModel.dismissFullscreenImage() },
                )
            }
        }
    } // end root Box

    // ── Table of Contents ──────────────────────────────────────────────────────
    if (showContents) {
        com.example.liber.core.designsystem.LiberModalBottomSheet(
            onDismissRequest = { showContents = false },
            title = UiText.StringResource(R.string.reader_nav_contents),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            ) {
                items(tocItems) { item ->
                    TocItemRow(
                        item = item,
                        onClick = {
                            item.mPath?.let { viewModel.goToXPointer(it) }
                            showContents = false
                            viewModel.toggleUI()
                        },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // ── Notebook ──────────────────────────────────────────────────────────────
    if (showNotebook) {
        var showExportMenu by remember { mutableStateOf(false) }
        com.example.liber.core.designsystem.LiberModalBottomSheet(
            onDismissRequest = { showNotebook = false },
            title = UiText.StringResource(R.string.reader_nav_notebook),
            actions = {
                if (annotations.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(
                                PhosphorIcons.Regular.ShareNetwork,
                                contentDescription = "Export",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Markdown (.md)") },
                                onClick = {
                                    showExportMenu = false
                                    exportAnnotations(
                                        context,
                                        exportScope,
                                        bookTitle,
                                        annotations,
                                        "markdown"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Plain Text (.txt)") },
                                onClick = {
                                    showExportMenu = false
                                    exportAnnotations(
                                        context,
                                        exportScope,
                                        bookTitle,
                                        annotations,
                                        "text"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("JSON (.json)") },
                                onClick = {
                                    showExportMenu = false
                                    exportAnnotations(
                                        context,
                                        exportScope,
                                        bookTitle,
                                        annotations,
                                        "json"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("HTML (.html)") },
                                onClick = {
                                    showExportMenu = false
                                    exportAnnotations(
                                        context,
                                        exportScope,
                                        bookTitle,
                                        annotations,
                                        "html"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) {
            NotebookView(
                bookmarks = bookmarks,
                annotations = annotations,
                modifier = Modifier.weight(1f),
                onBookmarkClick = { bm ->
                    val prog = bm.locator.toFloatOrNull()
                    if (prog != null) viewModel.goToProgress(prog)
                    showNotebook = false
                    viewModel.toggleUI()
                },
                onDeleteBookmark = { bm -> onDeleteBookmark(bm.id) },
                onNoteClick = { annotation ->
                    viewModel.goToXPointer(annotation.locator)
                    showNotebook = false
                    viewModel.toggleUI()
                },
                onDeleteNote = { annotation -> onDeleteAnnotation(annotation.id) },
            )
        }
    }

    // ── Themes & Settings ──────────────────────────────────────────────────────
    if (showThemes) {
        com.example.liber.core.designsystem.LiberModalBottomSheet(
            onDismissRequest = { showThemes = false },
            title = UiText.StringResource(R.string.reader_themes_title),
        ) {
            ThemesSheet(
                currentThemeId = themeId,
                onThemeChange = { id -> viewModel.setTheme(id) },
                fontSize = fontSize,
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
                onResetSettings = { viewModel.resetLayoutSettings() },
            )
        }
    }

    // ── Dictionary Lookup ───────────────────────────────────────────────────
    if (showDictionaryLookupSheet) {
        DictionaryLookupSheet(
            query = activeLookupQuery.orEmpty(),
            lookupState = dictionaryLookupState,
            onDismiss = {
                showDictionaryLookupSheet = false
                dictionaryViewModel.clearLookupState()
            },
        )
    }

    // ── Create / Edit Annotation ──────────────────────────────────────────────
    if (showAnnotationCreator) {
        val noteText by viewModel.annotationNoteText.collectAsState()
        val selectedColor by viewModel.annotationColorArgb.collectAsState()
        val selectedText by viewModel.pendingSelectedText.collectAsState()
        val annotationType by viewModel.pendingAnnotationType.collectAsState()
        val sheetTitle = if (annotationType == "highlight") {
            UiText.StringResource(R.string.reader_annotation_highlight)
        } else {
            UiText.StringResource(R.string.reader_annotation_note)
        }

        com.example.liber.core.designsystem.LiberModalBottomSheet(
            onDismissRequest = { viewModel.cancelAnnotation() },
            title = sheetTitle,
        ) {
            CreateAnnotationSheet(
                annotationType = annotationType,
                selectedText = selectedText,
                noteText = noteText,
                selectedColorArgb = selectedColor,
                currentChapter = currentChapter,
                onNoteTextChange = { viewModel.setAnnotationNote(it) },
                onColorChange = { viewModel.setAnnotationColor(it) },
                onSave = {
                    val editingId = viewModel.editingAnnotationId.value
                    if (editingId != null) {
                        val existing = annotations.find { it.id == editingId }
                        if (existing != null) {
                            onSaveAnnotation(
                                existing.copy(
                                    type = if (noteText.isNotBlank()) AnnotationType.NOTE else AnnotationType.HIGHLIGHT,
                                    note = noteText.ifBlank { null },
                                    color = selectedColor,
                                )
                            )
                        }
                    } else {
                        val xptr = viewModel.pendingXPointer.value ?: viewModel.currentXPointer()
                        ?: run { viewModel.cancelAnnotation(); return@CreateAnnotationSheet }
                        onSaveAnnotation(
                            Annotation(
                                bookId = bookId,
                                type = if (annotationType.lowercase() == "note") AnnotationType.NOTE else AnnotationType.HIGHLIGHT,
                                color = selectedColor,
                                locator = xptr,
                                endLocator = viewModel.pendingEndXPointer.value ?: "",
                                text = selectedText,
                                note = noteText.ifBlank { null },
                            )
                        )
                    }
                    viewModel.cancelAnnotation()
                },
                onCancel = { viewModel.cancelAnnotation() },
            )
        }
    }

    // ── Annotation Action Sheet (tap on existing highlight) ───────────────────
    if (tappedAnnotation != null) {
        val sheetTitle = if (tappedAnnotation.type == AnnotationType.HIGHLIGHT) {
            UiText.StringResource(R.string.reader_annotation_highlight)
        } else {
            UiText.StringResource(R.string.reader_annotation_note)
        }
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
                        val shareText = buildString {
                            append("\u201C").append(text).append("\u201D\n\n")
                            append(context.getString(R.string.reader_share_excerpt_from))
                            append("\u201C").append(bookTitle).append(".\u201D Liber.\n\n")
                            append(context.getString(R.string.reader_share_copyright_notice))
                        }
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                },
                                null
                            )
                        )
                    }
                    viewModel.dismissAnnotationMenu()
                },
                onDelete = {
                    onDeleteAnnotation(tappedAnnotation.id)
                    viewModel.dismissAnnotationMenu()
                },
            )
        }
    }
}

// ── Export Annotations ───────────────────────────────────────────────────────

private fun exportAnnotations(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    bookTitle: String,
    annotations: List<Annotation>,
    format: String = "markdown"
) {
    if (annotations.isEmpty()) return

    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val content = when (format) {
            "markdown" -> buildString {
                append("# $bookTitle\n\n")
                append("## Annotations\n\n")
                annotations.sortedBy { it.createdAt }.forEach { annotation ->
                    if (!annotation.text.isNullOrBlank()) {
                        append("> ${annotation.text}\n\n")
                    }
                    if (!annotation.note.isNullOrBlank()) {
                        append("${annotation.note}\n\n")
                    }
                    append("---\n\n")
                }
            }

            "json" -> {
                val items = annotations.map { annotation ->
                    val text = annotation.text?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""
                    val note = annotation.note?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""
                    "{\"text\":\"$text\",\"note\":\"$note\",\"type\":\"${annotation.type}\"}"
                }
                "[\n  ${items.joinToString(",\n  ")}\n]"
            }

            "html" -> buildString {
                append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>$bookTitle - Annotations</title>")
                append("<style>body{font-family:sans-serif;max-width:800px;margin:40px auto;padding:0 20px;line-height:1.6;}")
                append("blockquote{border-left:4px solid #ccc;padding-left:16px;margin:20px 0;font-style:italic;}")
                append("hr{border:0;border-top:1px solid #eee;margin:40px 0;}</style></head><body>")
                append("<h1>$bookTitle</h1>")
                annotations.sortedBy { it.createdAt }.forEach { annotation ->
                    if (!annotation.text.isNullOrBlank()) {
                        append("<blockquote>${annotation.text}</blockquote>")
                    }
                    if (!annotation.note.isNullOrBlank()) {
                        append("<p>${annotation.note}</p>")
                    }
                    append("<hr>")
                }
                append("</body></html>")
            }

            else -> buildString { // txt
                append("$bookTitle - Annotations\n\n")
                annotations.sortedBy { it.createdAt }.forEach { annotation ->
                    if (!annotation.text.isNullOrBlank()) {
                        append("\"${annotation.text}\"\n")
                    }
                    if (!annotation.note.isNullOrBlank()) {
                        append("Note: ${annotation.note}\n")
                    }
                    append("\n-------------------\n\n")
                }
            }
        }

        val extension = when (format) {
            "markdown" -> "md"
            "json" -> "json"
            "html" -> "html"
            else -> "txt"
        }
        val mimeType = when (format) {
            "markdown" -> "text/markdown"
            "json" -> "application/json"
            "html" -> "text/html"
            else -> "text/plain"
        }

        val fileName = "${bookTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")}_annotations.$extension"
        val file = java.io.File(context.cacheDir, fileName)
        try {
            file.writeText(content)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_SUBJECT, "$bookTitle - Annotations")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Annotations"))
        } catch (e: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    "Failed to export: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

// ── TOC row ───────────────────────────────────────────────────────────────────

@Composable
private fun TocItemRow(item: TOCItem, onClick: () -> Unit) {
    val depth = (item.mLevel - 1).coerceAtLeast(0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            text = item.mName ?: stringResource(R.string.reader_label_untitled),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (depth == 0) FontWeight.SemiBold else FontWeight.Normal,
            color = if (depth == 0) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

// ── Progress Scrubber ─────────────────────────────────────────────────────────

@Composable
private fun ProgressScrubber(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
) {
    var draggingProgress by remember { mutableStateOf<Float?>(null) }
    val displayProgress = draggingProgress ?: progress

    Canvas(
        modifier = modifier
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onProgressChange((offset.x / size.width).coerceIn(0f, 1f))
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
                    onDragCancel = { draggingProgress = null },
                    onDrag = { change, _ ->
                        draggingProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        val trackH = 4.dp.toPx()
        val trackY = (size.height - trackH) / 2f
        val clamped = displayProgress.coerceIn(0f, 1f)

        drawRoundRect(
            color = trackColor, topLeft = Offset(0f, trackY),
            size = Size(size.width, trackH), cornerRadius = CornerRadius(trackH / 2f),
        )
        val fillW = size.width * clamped
        if (fillW > 0f) {
            drawRoundRect(
                color = progressColor, topLeft = Offset(0f, trackY),
                size = Size(fillW, trackH), cornerRadius = CornerRadius(trackH / 2f),
            )
        }
        val thumbR = 8.dp.toPx()
        val thumbX = fillW.coerceIn(thumbR, size.width - thumbR)

        drawCircle(
            color = progressColor,
            radius = thumbR,
            center = Offset(thumbX, size.height / 2f)
        )
    }
}

// ── SelectionHandle ───────────────────────────────────────────────────────────

@Composable
private fun SelectionHandle(
    anchor: SelectionAnchor,
    isStart: Boolean,
    onDrag: (x: Int, y: Int) -> Unit,
    onDragEnd: () -> Unit = {},
) {
    val density = LocalDensity.current
    val handleSizePx = with(density) { 20.dp.roundToPx() }
    val color = MaterialTheme.colorScheme.primary

    // Not keyed on anchor so cumulative drag survives anchor updates during a drag session.
    // LaunchedEffect resets the offset whenever a new selection is established (anchor changes).
    var cumDx by remember { mutableStateOf(0f) }
    var cumDy by remember { mutableStateOf(0f) }
    LaunchedEffect(anchor) {
        cumDx = 0f
        cumDy = 0f
    }

    Box(
        modifier = Modifier
            .offset {
                val baseX = if (isStart) anchor.x.toInt() - handleSizePx else anchor.x.toInt()
                IntOffset(baseX + cumDx.toInt(), anchor.y.toInt() + cumDy.toInt())
            }
            .size(40.dp)
            .pointerInput(anchor) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        cumDx += dragAmount.x
                        cumDy += dragAmount.y
                        val queryX =
                            anchor.x + (if (isStart) -handleSizePx / 2f else handleSizePx / 2f) + cumDx
                        val queryY = anchor.y + cumDy
                        onDrag(queryX.toInt(), queryY.toInt())
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color, CircleShape)
        )
    }
}

// ── ReaderNavItem ─────────────────────────────────────────────────────────────

@Composable
internal fun ReaderNavItem(
    icon: @Composable () -> Unit,
    label: UiText,
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
            text = label.asString(),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontSize = 10.sp,
        )
    }
}

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.adamglin.phosphoricons.regular.TextAa
import com.example.liber.R
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.AnnotationType
import com.example.liber.data.repository.UserPreferencesRepository
import com.example.liber.feature.reader.components.AnnotationActionsSheet
import com.example.liber.feature.reader.components.CreateAnnotationSheet
import com.example.liber.feature.reader.components.HighlightColorPicker
import com.example.liber.feature.reader.components.NotebookView
import com.example.liber.feature.reader.components.SearchView
import com.example.liber.feature.reader.components.SelectionActionsMenu
import com.example.liber.feature.reader.components.ThemesSheet
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
    val viewModel: ReaderViewModel = viewModel(
        key = bookId,
        factory = ReaderViewModel.Factory(
            application,
            bookUri,
            bookTitle,
            bookId,
            userPreferencesRepository
        )
    )
    val context = LocalContext.current
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
    val columnCount by viewModel.columnCount.collectAsState()
    val justifyText by viewModel.justifyText.collectAsState()
    val selectionActive by viewModel.selectionActive.collectAsState()
    val showSelectionMenu by viewModel.showSelectionMenu.collectAsState()

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

    // Re-apply engine settings whenever preferences change
    LaunchedEffect(
        themeId, fontSize, pageScroll, customizeLayout,
        lineSpacing, characterSpacing, wordSpacing, margins, columnCount, justifyText
    ) {
        viewModel.applyCurrentSettings()
        viewModel.redraw()
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
                (tappedAnnotationId != null)

    // ── Derived state ────────────────────────────────────────────────────────

    // Current chapter title from TOC (last item whose page <= current page)
    val currentChapter = remember(positionProps, tocItems) {
        val page = positionProps?.pageNumber ?: 0
        tocItems.lastOrNull { it.mPage <= page }?.mName
    }

    // Bookmark detection: bookmarks store progress as a float string ("0.35")
    val isCurrentPageBookmarked = remember(progress, bookmarks) {
        bookmarks.any { bm ->
            val bmProg = bm.locator.toFloatOrNull() ?: return@any false
            abs(bmProg - progress) < 0.03f
        }
    }

    // ── UI chrome colors ─────────────────────────────────────────────────────
    val colorScheme = MaterialTheme.colorScheme
    val chromeBg = colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
    val chromeIcon = colorScheme.onSurfaceVariant
    val chromeOnIcon = colorScheme.onSurface
    val chromeDivider = colorScheme.outlineVariant
    val chromeLabel = colorScheme.onSurfaceVariant

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

                                if (pageScroll) {
                                    // Scroll mode — start tracking drag immediately (no long-press wait)
                                    var prevY = startY
                                    drag(down.id) { change ->
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
                                        // No drag detected → treat as tap
                                        viewModel.toggleUI()
                                    }
                                } else {
                                    val longPress = awaitLongPressOrCancellation(down.id)

                                    if (longPress != null) {
                                        // Long-press → text selection
                                        viewModel.startTextSelection(startX.toInt(), startY.toInt())
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
                                    } else {
                                        // Page mode — horizontal swipe or tap zones
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
                        },
                )
            }
        }

        // ── Top Bar ──────────────────────────────────────────────────────────
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
                    IconButton(onClick = handleBack) {
                        Icon(
                            PhosphorIcons.Regular.ArrowLeft,
                            contentDescription = "Back",
                            tint = chromeOnIcon,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = chromeLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (currentChapter != null) {
                            Text(
                                text = currentChapter,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = chromeOnIcon,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (isCurrentPageBookmarked) {
                                val match = bookmarks.minByOrNull { bm ->
                                    abs((bm.locator.toFloatOrNull() ?: Float.MAX_VALUE) - progress)
                                }
                                match?.let { onDeleteBookmark(it.id) }
                            } else {
                                onSaveBookmark(
                                    BookmarkModel(
                                        bookId = bookId,
                                        locator = progress.toString(),
                                        chapter = currentChapter,
                                    )
                                )
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .align(Alignment.BottomCenter)
                        .background(chromeDivider)
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

        // ── Selection Actions Menu ────────────────────────────────────────────
        if (showSelectionMenu) {
            val pendingText by viewModel.pendingSelectedText.collectAsState()
            SelectionActionsMenu(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                onHighlight = { viewModel.onSelectionMenuHighlight() },
                onNote = { viewModel.onSelectionMenuNote() },
                onShare = {
                    val text = pendingText
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
                                null,
                            )
                        )
                    }
                    viewModel.dismissSelectionMenu()
                },
                onDismiss = { viewModel.dismissSelectionMenu() },
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(chromeDivider)
                    )
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
                            onProgressChange = { viewModel.goToProgress(it) },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.labelSmall,
                            color = chromeLabel,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        ReaderNavItem(
                            icon = { Icon(PhosphorIcons.Regular.List, null, tint = chromeIcon) },
                            label = UiText.StringResource(R.string.reader_nav_contents),
                            labelColor = chromeLabel,
                            onClick = { showContents = true },
                        )
                        ReaderNavItem(
                            icon = {
                                Icon(
                                    PhosphorIcons.Regular.MagnifyingGlass,
                                    null,
                                    tint = chromeIcon
                                )
                            },
                            label = UiText.StringResource(R.string.reader_nav_search),
                            labelColor = chromeLabel,
                            onClick = { showSearch = true },
                        )
                        ReaderNavItem(
                            icon = {
                                Box {
                                    Icon(PhosphorIcons.Regular.NotePencil, null, tint = chromeIcon)
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
                            label = UiText.StringResource(R.string.reader_nav_notebook),
                            labelColor = chromeLabel,
                            onClick = { showNotebook = true },
                        )
                        ReaderNavItem(
                            icon = { Icon(PhosphorIcons.Regular.TextAa, null, tint = chromeIcon) },
                            label = UiText.StringResource(R.string.reader_nav_themes),
                            labelColor = chromeLabel,
                            onClick = { showThemes = true },
                        )
                    }
                }
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

    // ── Search ────────────────────────────────────────────────────────────────
    if (showSearch) {
        com.example.liber.core.designsystem.LiberModalBottomSheet(
            onDismissRequest = {
                viewModel.clearSearch()
                showSearch = false
            },
            title = UiText.StringResource(R.string.reader_search_title),
        ) {
            SearchView(
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
                onDismiss = {
                    viewModel.clearSearch()
                    showSearch = false
                },
            )
        }
    }

    // ── Notebook ──────────────────────────────────────────────────────────────
    if (showNotebook) {
        com.example.liber.core.designsystem.LiberModalBottomSheet(
            onDismissRequest = { showNotebook = false },
            title = UiText.StringResource(R.string.reader_nav_notebook),
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
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val progressColor = MaterialTheme.colorScheme.onSurfaceVariant
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
        val thumbR = 10.dp.toPx()
        val thumbX = fillW.coerceIn(thumbR, size.width - thumbR)
        drawCircle(
            color = Color.Black.copy(alpha = 0.15f),
            radius = thumbR + 1.dp.toPx(),
            center = Offset(thumbX, size.height / 2f + 1.dp.toPx()),
        )
        drawCircle(color = Color.White, radius = thumbR, center = Offset(thumbX, size.height / 2f))
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

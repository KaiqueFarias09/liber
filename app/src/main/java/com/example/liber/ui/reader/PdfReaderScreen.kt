package com.example.liber.ui.reader

import android.graphics.Matrix
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Bookmark
import com.adamglin.phosphoricons.regular.List
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.NotePencil
import com.adamglin.phosphoricons.regular.PencilSimple
import com.example.liber.data.AnnotationEntity
import com.example.liber.data.BookmarkEntity
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PdfReaderScreen(
    uri: Uri,
    title: String,
    bookId: String,
    initialPage: Int,
    bookmarks: List<BookmarkEntity>,
    notes: List<AnnotationEntity>,
    onSaveLocator: (json: String, progress: Int) -> Unit,
    onSaveBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onSaveNote: (AnnotationEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fragmentActivity = LocalContext.current as FragmentActivity
    val viewModel: PdfReaderViewModel = viewModel()

    val showUI by viewModel.showUI.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val inkConfig by viewModel.inkConfig.collectAsState()
    val drawMode by viewModel.drawingModeActive.collectAsState()
    val showNoteCreator by viewModel.showNoteCreator.collectAsState()
    val writableUri by viewModel.writableUri.collectAsState()

    var showContents by remember { mutableStateOf(false) }
    var showNotebook by remember { mutableStateOf(false) }
    var showDrawPanel by remember { mutableStateOf(false) }

    val isAnyModalOpen = showContents || showNotebook || showDrawPanel || showNoteCreator

    // Page-based bookmark detection
    val isCurrentPageBookmarked = bookmarks.any { bm ->
        runCatching { JSONObject(bm.locator).getInt("page") == currentPage }.getOrDefault(false)
    }

    // Prepare writable URI on first open
    LaunchedEffect(uri, bookId) { viewModel.prepareWritableUri(uri, bookId) }

    // Fragment reference for programmatic control
    var pdfFragment by remember { mutableStateOf<LibPdfViewerFragment?>(null) }
    var hasRestoredPage by remember { mutableStateOf(false) }

    // Pending scroll request (set by TOC / jump-to-page)
    var pendingScrollPage by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pendingScrollPage) {
        val page = pendingScrollPage ?: return@LaunchedEffect
        pdfFragment?.scrollToPage(page)
        pendingScrollPage = null
    }

    fun handleBack() {
        val prog = if (totalPages > 0) currentPage * 100 / totalPages else 0
        onSaveLocator("""{"page":$currentPage}""", prog)
        onBack()
    }

    BackHandler {
        if (pdfFragment?.isTextSearchActive == true) {
            pdfFragment?.isTextSearchActive = false
            viewModel.showUI()
        } else {
            handleBack()
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val chromeBg = colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
    val chromeIcon = colorScheme.onSurfaceVariant
    val chromeLabel = colorScheme.onSurfaceVariant

    Box(modifier = modifier.fillMaxSize()) {

        // ── PDF Fragment ──────────────────────────────────────────────────────
        val currentWritableUri = writableUri
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                FragmentContainerView(ctx).apply {
                    id = View.generateViewId()
                    val frag = LibPdfViewerFragment()
                    frag.onDocumentLoaded = { count ->
                        viewModel.onDocumentLoaded(count)
                        if (!hasRestoredPage && initialPage > 0) {
                            frag.scrollToPage(initialPage)
                            hasRestoredPage = true
                        }
                    }
                    frag.onPageChanged = { page -> viewModel.onPageChanged(page) }
                    fragmentActivity.supportFragmentManager.commit { replace(id, frag) }
                    pdfFragment = frag
                }
            },
            update = { _ ->
                val wu = currentWritableUri ?: return@AndroidView
                val frag = pdfFragment ?: return@AndroidView
                if (frag.documentUri != wu) frag.documentUri = wu
            },
        )

        // ── Ink Drawing Overlay ───────────────────────────────────────────────
        if (drawMode) {
            InkDrawingOverlay(
                inkConfig = inkConfig,
                modifier = Modifier.fillMaxSize(),
            )
        }

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
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = ::handleBack) {
                            Icon(
                                PhosphorIcons.Regular.ArrowLeft,
                                contentDescription = "Back",
                                tint = chromeIcon
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                color = chromeLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (totalPages > 0) {
                                Text(
                                    text = "Page ${currentPage + 1} of $totalPages",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                )
                            }
                        }

                        // Bookmark toggle
                        IconButton(
                            onClick = {
                                if (isCurrentPageBookmarked) {
                                    bookmarks.find { bm ->
                                        runCatching { JSONObject(bm.locator).getInt("page") == currentPage }
                                            .getOrDefault(false)
                                    }?.let { onDeleteBookmark(it.id) }
                                } else {
                                    onSaveBookmark(
                                        BookmarkEntity(
                                            bookId = bookId,
                                            locator = """{"page":$currentPage}""",
                                            chapter = "Page ${currentPage + 1}",
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
                    // Bottom divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .align(Alignment.BottomCenter)
                            .background(colorScheme.outlineVariant)
                    )
                }
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
                            .background(colorScheme.outlineVariant)
                    )

                    // Page scrubber row
                    if (totalPages > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "${currentPage + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = chromeLabel,
                            )
                            PdfProgressScrubber(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                onPageChange = { page -> pdfFragment?.scrollToPage(page) },
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "$totalPages",
                                style = MaterialTheme.typography.labelSmall,
                                color = chromeLabel,
                            )
                        }
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
                            onClick = {
                                pdfFragment?.isTextSearchActive = true
                                viewModel.toggleUI()
                            },
                        )
                        ReaderNavItem(
                            icon = {
                                Box {
                                    Icon(
                                        PhosphorIcons.Regular.NotePencil,
                                        contentDescription = null,
                                        tint = chromeIcon
                                    )
                                    if (bookmarks.isNotEmpty() || notes.isNotEmpty()) {
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
                                    PhosphorIcons.Regular.PencilSimple, contentDescription = null,
                                    tint = if (drawMode) colorScheme.primary else chromeIcon,
                                )
                            },
                            label = if (drawMode) "Drawing" else "Draw",
                            labelColor = if (drawMode) colorScheme.primary else chromeLabel,
                            onClick = { showDrawPanel = true },
                        )
                    }
                }
            }
        }
    }

    // ── Contents Sheet ────────────────────────────────────────────────────────
    if (showContents) {
        ModalBottomSheet(
            onDismissRequest = { showContents = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            DarkSheetHeader(title = "Contents", onClose = { showContents = false })

            var contentsTab by remember { mutableStateOf("bookmarks") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = contentsTab == "bookmarks",
                    onClick = { contentsTab = "bookmarks" },
                    label = { Text("Bookmarks") },
                )
                FilterChip(
                    selected = contentsTab == "jump",
                    onClick = { contentsTab = "jump" },
                    label = { Text("Jump to Page") },
                )
            }

            when (contentsTab) {
                "bookmarks" -> {
                    if (bookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No bookmarks yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        BookmarksView(
                            bookmarks = bookmarks,
                            onBookmarkClick = { bm ->
                                val page =
                                    runCatching { JSONObject(bm.locator).getInt("page") }.getOrDefault(
                                        0
                                    )
                                pendingScrollPage = page
                                showContents = false
                            },
                            onDeleteBookmark = { bm -> onDeleteBookmark(bm.id) },
                        )
                    }
                }

                "jump" -> {
                    var jumpInput by remember { mutableStateOf("") }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Enter a page number (1–$totalPages)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = jumpInput,
                                onValueChange = { jumpInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Page") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    val page = jumpInput.toIntOrNull()?.minus(1)
                                    if (page != null && page in 0 until totalPages) {
                                        pendingScrollPage = page
                                        showContents = false
                                    }
                                }
                            ) { Text("Go") }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Notebook Sheet ────────────────────────────────────────────────────────
    if (showNotebook) {
        ModalBottomSheet(
            onDismissRequest = { showNotebook = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            DarkSheetHeader(title = "Notebook", onClose = { showNotebook = false })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Button(
                    onClick = {
                        showNotebook = false
                        viewModel.openNoteCreator()
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) { Text("Add Note") }
            }

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No notes yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(notes, key = { it.id }) { note ->
                        DarkAnnotationItem(
                            annotation = note,
                            onClick = {
                                val page =
                                    runCatching { JSONObject(note.locator).getInt("page") }.getOrDefault(
                                        0
                                    )
                                pendingScrollPage = page
                                showNotebook = false
                            },
                            onDelete = { onDeleteNote(note.id) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Draw Panel Sheet ──────────────────────────────────────────────────────
    if (showDrawPanel) {
        ModalBottomSheet(
            onDismissRequest = { showDrawPanel = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            DarkSheetHeader(title = "Drawing Tools", onClose = { showDrawPanel = false })

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Tool selector
                Text(
                    "Tool",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        PdfInkTool.PEN to "Pen",
                        PdfInkTool.HIGHLIGHTER to "Highlighter",
                        PdfInkTool.ERASER to "Eraser"
                    )
                        .forEach { (tool, label) ->
                            FilterChip(
                                selected = inkConfig.tool == tool,
                                onClick = { viewModel.setInkTool(tool) },
                                label = { Text(label) },
                            )
                        }
                }

                // Color picker
                Text(
                    "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AnnotationColorOptions) { colorArgb ->
                        val selected = inkConfig.color == colorArgb
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorArgb.toLong() and 0xFFFFFFFFL))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape,
                                )
                                .clickable { viewModel.setInkColor(colorArgb) },
                        )
                    }
                }

                // Thickness slider
                val thickness = inkConfig.thickness
                Text(
                    "Thickness: ${thickness.toInt()}px",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = thickness,
                    onValueChange = { viewModel.setInkThickness(it) },
                    valueRange = 2f..20f,
                )

                // Toggle drawing mode
                if (drawMode) {
                    OutlinedButton(
                        onClick = { viewModel.exitDrawingMode(); showDrawPanel = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Exit Drawing Mode") }
                } else {
                    Button(
                        onClick = { viewModel.toggleDrawingMode(); showDrawPanel = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Enter Drawing Mode") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Note Creator Sheet ────────────────────────────────────────────────────
    if (showNoteCreator) {
        val noteText by viewModel.pendingNoteText.collectAsState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissNoteCreator() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            DarkSheetHeader(title = "Add Note", onClose = { viewModel.dismissNoteCreator() })
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Page ${currentPage + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { viewModel.setNoteText(it) },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.dismissNoteCreator() },
                        modifier = Modifier.weight(1f),
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            onSaveNote(
                                AnnotationEntity(
                                    bookId = bookId,
                                    type = "note",
                                    color = 0xFFFFF8DC.toInt(),
                                    locator = """{"page":$currentPage}""",
                                    text = "Page ${currentPage + 1}",
                                    note = noteText.ifBlank { null },
                                )
                            )
                            viewModel.dismissNoteCreator()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = noteText.isNotBlank(),
                    ) { Text("Save") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Ink Drawing Overlay ───────────────────────────────────────────────────────

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun InkDrawingOverlay(
    inkConfig: PdfInkConfig,
    modifier: Modifier = Modifier,
) {
    val finishedStrokes = remember { mutableStateListOf<Stroke>() }
    val inProgressView = remember { mutableStateOf<InProgressStrokesView?>(null) }

    val brush = remember(inkConfig) {
        when (inkConfig.tool) {
            PdfInkTool.ERASER -> null
            PdfInkTool.HIGHLIGHTER -> {
                val highlightColor =
                    Color(inkConfig.color.toLong() and 0xFFFFFFFFL).copy(alpha = 0.5f)
                Brush.createWithColorIntArgb(
                    family = StockBrushes.marker(),
                    colorIntArgb = highlightColor.toArgb(),
                    size = inkConfig.thickness * 2.5f,
                    epsilon = 0.1f,
                )
            }

            PdfInkTool.PEN ->
                Brush.createWithColorIntArgb(
                    family = StockBrushes.pressurePen(),
                    colorIntArgb = inkConfig.color,
                    size = inkConfig.thickness,
                    epsilon = 0.1f,
                )
        }
    }

    Box(modifier = modifier) {
        // Render finished strokes on a Compose Canvas
        val strokeRenderer = remember { CanvasStrokeRenderer.create() }
        val identityMatrix = remember { Matrix() }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { composeCanvas ->
                finishedStrokes.forEach { stroke ->
                    strokeRenderer.draw(composeCanvas.nativeCanvas, stroke, identityMatrix)
                }
            }
        }

        // InProgressStrokesView intercepts touch events and renders active strokes
        AndroidView(
            factory = { ctx ->
                InProgressStrokesView(ctx).apply {
                    addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
                        override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                            finishedStrokes.addAll(strokes.values)
                            removeFinishedStrokes(strokes.keys)
                        }
                    })
                }.also { inProgressView.value = it }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    val view = inProgressView.value ?: return@pointerInteropFilter false
                    val activeBrush = brush ?: return@pointerInteropFilter false

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            val pointerId = event.getPointerId(event.actionIndex)
                            view.startStroke(event, pointerId, activeBrush)
                        }

                        MotionEvent.ACTION_MOVE -> {
                            for (i in 0 until event.pointerCount) {
                                view.addToStroke(event, event.getPointerId(i))
                            }
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                            val pointerId = event.getPointerId(event.actionIndex)
                            view.finishStroke(event, pointerId)
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            for (i in 0 until event.pointerCount) {
                                view.cancelStroke(event, event.getPointerId(i))
                            }
                        }
                    }
                    true
                },
        )
    }
}

// ── PDF Progress Scrubber ─────────────────────────────────────────────────────

@Composable
private fun PdfProgressScrubber(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val progressColor = MaterialTheme.colorScheme.onSurfaceVariant
    var draggingProgress by remember { mutableStateOf<Float?>(null) }

    val displayProgress = draggingProgress
        ?: if (totalPages > 1) currentPage.toFloat() / (totalPages - 1) else 0f

    Canvas(
        modifier = modifier
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val p = (offset.x / size.width).coerceIn(0f, 1f)
                    val page = (p * (totalPages - 1)).toInt().coerceIn(0, totalPages - 1)
                    onPageChange(page)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        draggingProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        draggingProgress?.let { p ->
                            val page = (p * (totalPages - 1)).toInt().coerceIn(0, totalPages - 1)
                            onPageChange(page)
                        }
                        draggingProgress = null
                    },
                    onDragCancel = { draggingProgress = null },
                    onDrag = { change, _ ->
                        draggingProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                )
            }
    ) {
        val trackH = 4.dp.toPx()
        val trackY = (size.height - trackH) / 2f
        val clampedProg = displayProgress.coerceIn(0f, 1f)

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, trackY),
            size = Size(size.width, trackH),
            cornerRadius = CornerRadius(trackH / 2f),
        )

        val fillW = size.width * clampedProg
        if (fillW > 0f) {
            drawRoundRect(
                color = progressColor,
                topLeft = Offset(0f, trackY),
                size = Size(fillW, trackH),
                cornerRadius = CornerRadius(trackH / 2f),
            )
        }

        val thumbR = 10.dp.toPx()
        val thumbX = fillW.coerceIn(thumbR, size.width - thumbR)
        drawCircle(
            color = Color.Black.copy(alpha = 0.15f),
            radius = thumbR + 1.dp.toPx(),
            center = Offset(thumbX, size.height / 2f + 1.dp.toPx()),
        )
        drawCircle(
            color = Color.White,
            radius = thumbR,
            center = Offset(thumbX, size.height / 2f),
        )
    }
}

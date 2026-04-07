package com.example.liber.ui.reader

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.view.GestureDetector
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.example.liber.data.InkStrokeEntity
import com.example.liber.ui.components.LiberModalBottomSheet
import org.json.JSONArray
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
    val persistedStrokes by viewModel.persistedStrokes.collectAsState()

    var showContents by remember { mutableStateOf(false) }
    var showNotebook by remember { mutableStateOf(false) }
    var showDrawPanel by remember { mutableStateOf(false) }

    // Hoisted here so strokes survive drawing-mode toggles
    val finishedStrokes = remember { mutableStateListOf<Stroke>() }

    // Tracks the screen rect of each visible page; updated by the fragment on every scroll/zoom.
    val pageLocations = remember { mutableStateOf(emptyMap<Int, android.graphics.RectF>()) }

    val isAnyModalOpen = showContents || showNotebook || showDrawPanel || showNoteCreator

    // Page-based bookmark detection
    val isCurrentPageBookmarked = bookmarks.any { bm ->
        runCatching { JSONObject(bm.locator).getInt("page") == currentPage }.getOrDefault(false)
    }

    // Prepare writable URI on first open and load any persisted ink strokes
    LaunchedEffect(uri, bookId) {
        viewModel.prepareWritableUri(uri, bookId)
        viewModel.loadStrokes(bookId)
    }

    // Fragment reference for programmatic control
    var pdfFragment by remember { mutableStateOf<LibPdfViewerFragment?>(null) }
    var hasRestoredPage by remember { mutableStateOf(false) }
    val appliedUri = remember { mutableStateOf<Uri?>(null) }

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
                    frag.onPageLocationsChanged = { locations -> pageLocations.value = locations }
                    fragmentActivity.supportFragmentManager.commit { replace(id, frag) }
                    pdfFragment = frag

                    // Detect single taps to toggle the chrome bars; pass all events through.
                    val tapDetector = GestureDetector(
                        ctx,
                        object : GestureDetector.SimpleOnGestureListener() {
                            override fun onSingleTapUp(e: MotionEvent): Boolean {
                                viewModel.toggleUI()
                                return true
                            }
                        },
                    )
                    setOnTouchListener { v, event ->
                        tapDetector.onTouchEvent(event)
                        // Always return false so the PDF fragment receives all events.
                        false
                    }
                }
            },
            update = { _ ->
                val wu = currentWritableUri ?: return@AndroidView
                val frag = pdfFragment ?: return@AndroidView
                if (appliedUri.value != wu) {
                    frag.setDocumentUriWhenReady(wu)
                    appliedUri.value = wu
                }
            },
        )

        // ── Ink Drawing Overlay ───────────────────────────────────────────────
        // Always composed so strokes are rendered even when drawing mode is off.
        // Only intercepts touch when drawing mode is on.
        InkDrawingOverlay(
            inkConfig = inkConfig,
            drawMode = drawMode,
            finishedStrokes = finishedStrokes,
            persistedStrokes = persistedStrokes,
            pageLocations = pageLocations.value,
            onStrokeCompleted = { stroke, config ->
                val entity = buildInkStrokeEntity(stroke, config, bookId, pageLocations.value)
                if (entity != null) viewModel.saveStroke(entity)
            },
            modifier = Modifier.fillMaxSize(),
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
        LiberModalBottomSheet(
            title = "Contents",
            onDismissRequest = { showContents = false },
            skipPartiallyExpanded = true,
        ) {
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
        LiberModalBottomSheet(
            title = "Notebook",
            onDismissRequest = { showNotebook = false },
            skipPartiallyExpanded = true,
        ) {
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
        LiberModalBottomSheet(
            title = "Drawing Tools",
            onDismissRequest = { showDrawPanel = false },
            skipPartiallyExpanded = false,
        ) {
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
        LiberModalBottomSheet(
            title = "Add Note",
            onDismissRequest = { viewModel.dismissNoteCreator() },
            skipPartiallyExpanded = true,
        ) {
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
    drawMode: Boolean,
    finishedStrokes: androidx.compose.runtime.snapshots.SnapshotStateList<Stroke>,
    persistedStrokes: List<InkStrokeEntity>,
    pageLocations: Map<Int, android.graphics.RectF>,
    onStrokeCompleted: (stroke: Stroke, config: PdfInkConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inProgressView = remember { mutableStateOf<InProgressStrokesView?>(null) }

    // rememberUpdatedState so the factory-created listener always calls the latest lambda
    val currentOnStrokeCompleted = rememberUpdatedState(onStrokeCompleted)
    val currentInkConfig = rememberUpdatedState(inkConfig)

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
        // ── Layer 1: Persisted strokes from previous sessions (Canvas Path) ──
        val persistedPaint = remember {
            Paint().apply {
                isAntiAlias = true; style = Paint.Style.STROKE; strokeCap =
                Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { composeCanvas ->
                val canvas = composeCanvas.nativeCanvas
                persistedStrokes.forEach { entity ->
                    val pageRect = pageLocations[entity.page] ?: return@forEach
                    val pw = pageRect.width()
                    val ph = pageRect.height()
                    if (pw <= 0f || ph <= 0f) return@forEach
                    val pts = parseStrokePoints(entity.pointsJson)
                    if (pts.size < 2) return@forEach

                    val path = Path()
                    pts.forEachIndexed { i, (nx, ny) ->
                        val sx = pageRect.left + nx * pw
                        val sy = pageRect.top + ny * ph
                        if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                    }

                    persistedPaint.color = entity.colorArgb
                    persistedPaint.strokeWidth = entity.strokeWidthFraction * pw
                    if (entity.isHighlighter) {
                        persistedPaint.alpha = 128
                        persistedPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                    } else {
                        persistedPaint.alpha = 255
                        persistedPaint.xfermode = null
                    }
                    canvas.drawPath(path, persistedPaint)
                }
            }
        }

        // ── Layer 2: Current-session finished strokes (CanvasStrokeRenderer) ──
        val strokeRenderer = remember { CanvasStrokeRenderer.create() }
        val identityMatrix = remember { Matrix() }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { composeCanvas ->
                finishedStrokes.forEach { stroke ->
                    strokeRenderer.draw(composeCanvas.nativeCanvas, stroke, identityMatrix)
                }
            }
        }

        // ── Layer 3: InProgressStrokesView — captures touch, renders live strokes ──
        // Only composed when drawing mode is active; otherwise it would sit on top of
        // the PDF fragment and block all scroll/zoom/tap gestures.
        if (drawMode) {
            AndroidView(
                factory = { ctx ->
                    InProgressStrokesView(ctx).apply {
                        addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
                            override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                                finishedStrokes.addAll(strokes.values)
                                removeFinishedStrokes(strokes.keys)
                                strokes.values.forEach { stroke ->
                                    currentOnStrokeCompleted.value(stroke, currentInkConfig.value)
                                }
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

// ── Ink stroke serialization helpers ─────────────────────────────────────────

/**
 * Converts a finished [Stroke] into a persistable [InkStrokeEntity] with page-normalized
 * coordinates. Returns null if no page can be determined from [pageLocations].
 *
 * Points are stored as fractions in [0, 1] relative to the page rect so that they render
 * correctly regardless of scroll position or zoom level.
 */
private fun buildInkStrokeEntity(
    stroke: Stroke,
    config: PdfInkConfig,
    bookId: String,
    pageLocations: Map<Int, android.graphics.RectF>,
): InkStrokeEntity? {
    if (pageLocations.isEmpty() || stroke.inputs.size == 0) return null

    // Compute stroke bounding box to find the best-matching page.
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    for (i in 0 until stroke.inputs.size) {
        val pt = stroke.inputs[i]
        if (pt.x < minX) minX = pt.x; if (pt.x > maxX) maxX = pt.x
        if (pt.y < minY) minY = pt.y; if (pt.y > maxY) maxY = pt.y
    }
    val strokeRect = android.graphics.RectF(minX, minY, maxX, maxY)

    // Pick the page whose rect has the greatest overlap with the stroke bounding box.
    var bestPage = -1
    var bestRect: android.graphics.RectF? = null
    var bestOverlap = -1f
    val intersection = android.graphics.RectF()
    for ((page, rect) in pageLocations) {
        val overlap = if (intersection.setIntersect(strokeRect, rect))
            intersection.width() * intersection.height() else 0f
        if (overlap > bestOverlap) {
            bestOverlap = overlap; bestPage = page; bestRect = rect
        }
    }
    // Fall back to closest page by centroid distance if no overlap found.
    if (bestPage == -1) {
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        var minDist = Float.MAX_VALUE
        for ((page, rect) in pageLocations) {
            val dx = maxOf(rect.left - cx, 0f, cx - rect.right)
            val dy = maxOf(rect.top - cy, 0f, cy - rect.bottom)
            val d = dx * dx + dy * dy
            if (d < minDist) {
                minDist = d; bestPage = page; bestRect = rect
            }
        }
    }
    val pageRect = bestRect ?: return null
    val pw = pageRect.width()
    val ph = pageRect.height()
    if (pw <= 0f || ph <= 0f) return null

    // Serialize points as normalized JSON.
    val sb = StringBuilder("[")
    for (i in 0 until stroke.inputs.size) {
        val pt = stroke.inputs[i]
        val nx = (pt.x - pageRect.left) / pw
        val ny = (pt.y - pageRect.top) / ph
        if (i > 0) sb.append(',')
        sb.append("""{"x":$nx,"y":$ny}""")
    }
    sb.append(']')

    val isHighlighter = config.tool == PdfInkTool.HIGHLIGHTER
    val rawWidth = if (isHighlighter) config.thickness * 2.5f else config.thickness

    return InkStrokeEntity(
        bookId = bookId,
        page = bestPage,
        strokeWidthFraction = rawWidth / pw,
        colorArgb = config.color,
        isHighlighter = isHighlighter,
        pointsJson = sb.toString(),
    )
}

/** Deserializes the JSON point array stored in [InkStrokeEntity.pointsJson]. */
private fun parseStrokePoints(json: String): List<Pair<Float, Float>> {
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            obj.getDouble("x").toFloat() to obj.getDouble("y").toFloat()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

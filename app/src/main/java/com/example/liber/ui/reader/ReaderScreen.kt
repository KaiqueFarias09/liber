package com.example.liber.ui.reader

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.adamglin.phosphoricons.regular.List
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.NotePencil
import com.adamglin.phosphoricons.regular.Trash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
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
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ID_HIGHLIGHT = 9_001
private const val ID_ADD_NOTE  = 9_002

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
 * Full-screen EPUB reader with toggleable top/bottom bars for
 * Table of Contents, Search, and Notes (annotations).
 */
@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    publication: Publication,
    bookId: String,
    initialLocatorJson: String?,
    annotations: List<AnnotationEntity>,
    pendingAnnotationRequest: AnnotationRequest?,
    onRequestAnnotation: (AnnotationRequest) -> Unit,
    onSaveLocator: (json: String, progress: Int) -> Unit,
    onSaveAnnotation: (AnnotationEntity) -> Unit,
    onDeleteAnnotation: (Long) -> Unit,
    onClearPendingAnnotation: () -> Unit,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(factory = ReaderViewModel.Factory(publication))
) {
    val fragmentActivity = LocalActivity.current as FragmentActivity
    val showUI by viewModel.showUI.collectAsState()
    var showContents by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    val showAnnotationCreator by viewModel.showAnnotationCreator.collectAsState()

    // Bridge: when the selection callback posts a text-selection request, open the annotation creator
    LaunchedEffect(pendingAnnotationRequest) {
        pendingAnnotationRequest ?: return@LaunchedEffect
        val type = if (pendingAnnotationRequest is AnnotationRequest.Highlight) "highlight" else "note"
        viewModel.startAnnotation(type = type, prefilledText = pendingAnnotationRequest.selectedText)
        onClearPendingAnnotation()
    }

    var navigator by remember { mutableStateOf<VisualNavigator?>(null) }

    // Render stored annotations as highlights in the EPUB WebView via the Decorator API.
    // withStarted suspends until the fragment is fully attached (onStart), ensuring
    // applyDecorations is not called before the fragment's ViewModel is accessible.
    LaunchedEffect(navigator, annotations) {
        val dn = navigator as? DecorableNavigator ?: return@LaunchedEffect
        val fragment = navigator as? Fragment ?: return@LaunchedEffect
        val decorations = annotations.mapNotNull { annotation ->
            runCatching {
                val locator = Locator.fromJSON(org.json.JSONObject(annotation.locator))
                    ?: return@runCatching null
                Decoration(
                    id = annotation.id.toString(),
                    locator = locator,
                    style = Decoration.Style.Highlight(tint = annotation.color),
                )
            }.getOrNull()
        }
        fragment.lifecycle.withStarted { } // suspend until the fragment is STARTED
        dn.applyDecorations(decorations, "annotations")
    }

    val handleBack = {
        navigator?.currentLocator?.value?.let { locator ->
            val progress = ((locator.locations.totalProgression ?: 0.0) * 100).toInt()
            onSaveLocator(locator.toJSON().toString(), progress)
        }
        onBack()
    }

    BackHandler(onBack = handleBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = android.view.View.generateViewId()

                    val restoredLocator = initialLocatorJson?.let { json ->
                        runCatching { Locator.fromJSON(org.json.JSONObject(json)) }.getOrNull()
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

                    val navigatorFactory = EpubNavigatorFactory(publication)
                    val fragmentFactory = navigatorFactory.createFragmentFactory(
                        initialLocator = restoredLocator
                            ?: publication.readingOrder
                                .firstOrNull()
                                ?.let { publication.locatorFromLink(it) },
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

        // Overlay to detect taps and toggle UI — uses detectTapGestures so that
        // long-press (text selection) and swipe (page turn) do NOT trigger the toggle.
        Box(
            modifier = Modifier
                .fillMaxSize(0.6f)
                .align(Alignment.Center)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { viewModel.toggleUI() })
                }
        )

        // Top Bar
        AnimatedVisibility(
            visible = showUI,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = handleBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.ArrowLeft,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
            }
        }

        // Bottom Navigation Bar
        AnimatedVisibility(
            visible = showUI,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            NavigationBar(
                containerColor = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White,
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = Color.White,
                    unselectedTextColor = Color.White,
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Color.White.copy(alpha = 0.15f),
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showContents = true },
                    icon = { Icon(PhosphorIcons.Regular.List, contentDescription = "Contents") },
                    label = { Text("Contents") },
                    colors = navItemColors,
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showSearch = true },
                    icon = { Icon(PhosphorIcons.Regular.MagnifyingGlass, contentDescription = "Search") },
                    label = { Text("Search") },
                    colors = navItemColors,
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showNotes = true },
                    icon = { Icon(PhosphorIcons.Regular.NotePencil, contentDescription = "Notes") },
                    label = { Text("Notes") },
                    colors = navItemColors,
                )
            }
        }
    }

    // Table of Contents Bottom Sheet
    if (showContents) {
        ModalBottomSheet(
            onDismissRequest = { showContents = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            ContentsView(
                links = publication.tableOfContents,
                onLinkClick = { link ->
                    val locator = publication.locatorFromLink(link)
                    if (locator != null) {
                        navigator?.go(locator, animated = true)
                    }
                    showContents = false
                    viewModel.toggleUI()
                }
            )
        }
    }

    // Search Bottom Sheet
    if (showSearch) {
        ModalBottomSheet(
            onDismissRequest = { showSearch = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxHeight(0.9f),
        ) {
            SearchView(
                viewModel = viewModel,
                onResultClick = { locator ->
                    navigator?.go(locator, animated = true)
                    showSearch = false
                    viewModel.toggleUI()
                }
            )
        }
    }

    // Notes Bottom Sheet
    if (showNotes) {
        ModalBottomSheet(
            onDismissRequest = { showNotes = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxHeight(0.9f),
        ) {
            AnnotationsView(
                annotations = annotations,
                onAddNote = { viewModel.startAnnotation(type = "note") },
                onNoteClick = { annotation ->
                    runCatching {
                        val locator = Locator.fromJSON(org.json.JSONObject(annotation.locator))
                        if (locator != null) navigator?.go(locator, animated = true)
                    }
                    showNotes = false
                    viewModel.toggleUI()
                },
                onDeleteNote = { annotation -> onDeleteAnnotation(annotation.id) },
            )
        }
    }

    // Create / Highlight Annotation Sheet
    if (showAnnotationCreator) {
        val noteText by viewModel.annotationNoteText.collectAsState()
        val selectedColor by viewModel.annotationColorArgb.collectAsState()
        val selectedText by viewModel.pendingSelectedText.collectAsState()
        val annotationType by viewModel.pendingAnnotationType.collectAsState()

        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelAnnotation() },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
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
                    val locator = navigator?.currentLocator?.value ?: return@CreateAnnotationSheet
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
                    viewModel.cancelAnnotation()
                },
                onCancel = { viewModel.cancelAnnotation() },
            )
        }
    }
}

// ── ContentsView ──────────────────────────────────────────────────────────────

@Composable
fun ContentsView(
    links: List<Link>,
    onLinkClick: (Link) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Text(
                "Table of Contents",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        items(links) { link ->
            Text(
                text = link.title ?: "Untitled",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLinkClick(link) }
                    .padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            link.children.forEach { child ->
                Text(
                    text = child.title ?: "Untitled",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLinkClick(child) }
                        .padding(start = 24.dp, top = 8.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── SearchView ────────────────────────────────────────────────────────────────

@Composable
fun SearchView(
    viewModel: ReaderViewModel,
    onResultClick: (Locator) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = query,
            onValueChange = { viewModel.search(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search in book...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching && results.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results) { locator ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResultClick(locator) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = locator.title ?: "Unknown Chapter",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        locator.text.before?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Text(
                            locator.text.highlight ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        locator.text.after?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                item {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    } else if (results.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.loadNextResults() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Load more")
                        }
                    }
                }
            }
        }
    }
}

// ── AnnotationsView ───────────────────────────────────────────────────────────

@Composable
fun AnnotationsView(
    annotations: List<AnnotationEntity>,
    onAddNote: () -> Unit,
    onNoteClick: (AnnotationEntity) -> Unit,
    onDeleteNote: (AnnotationEntity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Notes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            FilledTonalButton(onClick = onAddNote) {
                Icon(
                    PhosphorIcons.Regular.NotePencil,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Add note")
            }
        }

        if (annotations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No notes yet.\nTap \"Add note\" to mark your current position.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(annotations, key = { it.id }) { annotation ->
                    AnnotationItem(
                        annotation = annotation,
                        onClick = { onNoteClick(annotation) },
                        onDelete = { onDeleteNote(annotation) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnotationItem(
    annotation: AnnotationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val annotationColor = Color(annotation.color.toLong() and 0xFFFFFFFFL)
    val dateStr = remember(annotation.createdAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(annotation.createdAt))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(12.dp)
                .background(annotationColor, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            annotation.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
            }
            if (annotation.note == null) {
                Text(
                    text = "No note — tap to navigate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                PhosphorIcons.Regular.Trash,
                contentDescription = "Delete note",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── CreateAnnotationSheet ─────────────────────────────────────────────────────

@Composable
fun CreateAnnotationSheet(
    annotationType: String,           // "note" or "highlight"
    selectedText: String?,            // text the user selected in the EPUB, if any
    noteText: String,
    selectedColorArgb: Int,
    currentChapter: String?,
    onNoteTextChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val isHighlight = annotationType == "highlight"
    val title = if (isHighlight) "Highlight" else "New Note"
    val saveLabel = if (isHighlight) "Save highlight" else "Save note"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        currentChapter?.let { chapter ->
            Text(
                text = chapter,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        // Selected text excerpt (shown when the user long-pressed text in the EPUB)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }

        // Color picker
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
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        )
                        .clickable { onColorChange(colorArgb) }
                )
            }
        }

        // Note text field
        OutlinedTextField(
            value = noteText,
            onValueChange = onNoteTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text(if (isHighlight) "Add a note… (optional)" else "Write your note… (optional)") },
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
            ) {
                Text(saveLabel)
            }
        }
    }
}

// ── Column with content padding extension ─────────────────────────────────────

@Composable
private fun Column(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.padding(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

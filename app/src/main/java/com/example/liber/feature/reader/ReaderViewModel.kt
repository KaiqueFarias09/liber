package com.example.liber.feature.reader

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.liber.core.logging.AppLogger
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.AnnotationType
import com.example.liber.data.repository.UserPreferencesRepository
import com.example.liber.feature.reader.engine.ReaderCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.coolreader.crengine.DocView
import org.coolreader.crengine.PositionProperties
import org.coolreader.crengine.Selection
import org.coolreader.crengine.TOCItem
import java.util.Properties

private const val TAG = "LiberSelection"

// Default annotation colour — yellow at 50 % opacity
private val DEFAULT_ANNOTATION_COLOR = 0x80FFD60A.toInt()

/** Screen-pixel rectangle for a single highlight line segment, with its ARGB color. */
data class HighlightRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val color: Int,
    val annotationId: Long,
)

/** Screen-pixel position for a draggable selection handle. */
data class SelectionAnchor(val x: Float, val y: Float)

private const val DEFAULT_LINE_SPACING = 1.4f
private const val DEFAULT_CHAR_SPACING = 0.0f
private const val DEFAULT_WORD_SPACING = 0.0f
private const val DEFAULT_MARGINS = 24.0f

class ReaderViewModel(
    application: Application,
    val bookUri: Uri,
    val bookTitle: String,
    val bookId: String,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appLogger: AppLogger,
) : AndroidViewModel(application) {

    // ── Native document view ─────────────────────────────────────────────────

    private val docView = DocView()

    // ── UI state ─────────────────────────────────────────────────────────────

    private val _showUI = MutableStateFlow(true)
    val showUI: StateFlow<Boolean> = _showUI

    fun toggleUI() {
        _showUI.value = !_showUI.value
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    private val _pageBitmap = MutableStateFlow<Bitmap?>(null)
    val pageBitmap: StateFlow<Bitmap?> = _pageBitmap

    /** Current PositionProperties from the engine, updated after each page turn. */
    private val _positionProps = MutableStateFlow<PositionProperties?>(null)
    val positionProps: StateFlow<PositionProperties?> = _positionProps

    val pageCount: Int get() = _positionProps.value?.pageCount ?: 0
    val currentPage: Int get() = _positionProps.value?.pageNumber ?: 0

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _highlightRects = MutableStateFlow<List<HighlightRect>>(emptyList())
    val highlightRects: StateFlow<List<HighlightRect>> = _highlightRects

    // ── View dimensions ──────────────────────────────────────────────────────

    private var viewWidth = 0
    private var viewHeight = 0
    private var currentBitmap: Bitmap? = null
    private var documentLoaded = false
    private var latestAnnotations: List<Annotation> = emptyList()

    // Debounced settings pipe: rapid changes (sliders, toggles) coalesce into one engine call.
    private val _settingsTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private fun scheduleApply() {
        _settingsTrigger.tryEmit(Unit)
    }

    // ── Theme & font size ────────────────────────────────────────────────────

    private val _themeId = MutableStateFlow("original")
    val themeId: StateFlow<String> = _themeId

    private val _fontSize = MutableStateFlow(1.0)
    val fontSize: StateFlow<Double> = _fontSize

    fun setTheme(id: String) {
        viewModelScope.launch { userPreferencesRepository.setReaderTheme(id) }
    }

    fun adjustFontSize(delta: Double) {
        viewModelScope.launch {
            val next = (_fontSize.value + delta).coerceIn(0.5, 2.0)
            userPreferencesRepository.setFontSize(next.toFloat())
        }
    }

    // ── Scroll / layout settings ─────────────────────────────────────────────

    private val _pageScroll = MutableStateFlow(false)
    val pageScroll: StateFlow<Boolean> = _pageScroll

    private val _customizeLayout = MutableStateFlow(false)
    val customizeLayout: StateFlow<Boolean> = _customizeLayout

    private val _lineSpacing = MutableStateFlow(DEFAULT_LINE_SPACING.toDouble())
    val lineSpacing: StateFlow<Double> = _lineSpacing

    private val _characterSpacing = MutableStateFlow(DEFAULT_CHAR_SPACING.toDouble())
    val characterSpacing: StateFlow<Double> = _characterSpacing

    private val _wordSpacing = MutableStateFlow(DEFAULT_WORD_SPACING.toDouble())
    val wordSpacing: StateFlow<Double> = _wordSpacing

    private val _margins = MutableStateFlow(DEFAULT_MARGINS.toDouble())
    val margins: StateFlow<Double> = _margins

    private val _columnCount = MutableStateFlow("auto")
    val columnCount: StateFlow<String> = _columnCount

    private val _justifyText = MutableStateFlow(false)
    val justifyText: StateFlow<Boolean> = _justifyText

    fun setPageScroll(v: Boolean) =
        viewModelScope.launch { userPreferencesRepository.setPageScroll(v) }

    fun setCustomizeLayout(v: Boolean) =
        viewModelScope.launch { userPreferencesRepository.setCustomizeLayout(v) }

    fun setLineSpacing(v: Double) =
        viewModelScope.launch { userPreferencesRepository.setLineSpacing(v.toFloat()) }

    fun setCharacterSpacing(v: Double) =
        viewModelScope.launch { userPreferencesRepository.setCharSpacing(v.toFloat()) }

    fun setWordSpacing(v: Double) =
        viewModelScope.launch { userPreferencesRepository.setWordSpacing(v.toFloat()) }

    fun setMargins(v: Double) =
        viewModelScope.launch { userPreferencesRepository.setMargins(v.toFloat()) }

    fun setColumnCount(v: String) =
        viewModelScope.launch { userPreferencesRepository.setColumnCount(v) }

    fun setJustifyText(v: Boolean) =
        viewModelScope.launch { userPreferencesRepository.setJustifyText(v) }

    fun resetLayoutSettings() =
        viewModelScope.launch { userPreferencesRepository.resetReaderSettings() }

    // ── TOC ──────────────────────────────────────────────────────────────────

    private val _tocItems = MutableStateFlow<List<TOCItem>>(emptyList())
    val tocItems: StateFlow<List<TOCItem>> = _tocItems

    // ── Search ───────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchNext(query: String) {
        if (query.isBlank()) return
        _searchQuery.value = query
        _isSearching.value = true
        viewModelScope.launch(Dispatchers.IO) {
            docView.findText(query, origin = 1, reverse = 0, caseInsensitive = 1)
            renderPage()
            _isSearching.value = false
        }
    }

    fun searchPrev(query: String) {
        if (query.isBlank()) return
        _searchQuery.value = query
        _isSearching.value = true
        viewModelScope.launch(Dispatchers.IO) {
            docView.findText(query, origin = 1, reverse = 1, caseInsensitive = 1)
            renderPage()
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        viewModelScope.launch(Dispatchers.IO) {
            docView.clearSelection()
            renderPage()
        }
    }

    // ── Annotation creation state ────────────────────────────────────────────

    private val _showAnnotationCreator = MutableStateFlow(false)
    val showAnnotationCreator: StateFlow<Boolean> = _showAnnotationCreator

    private val _showHighlightColorPicker = MutableStateFlow(false)
    val showHighlightColorPicker: StateFlow<Boolean> = _showHighlightColorPicker

    private val _pendingSelectedText = MutableStateFlow<String?>(null)
    val pendingSelectedText: StateFlow<String?> = _pendingSelectedText

    private val _pendingXPointer = MutableStateFlow<String?>(null)
    val pendingXPointer: StateFlow<String?> = _pendingXPointer

    private val _pendingEndXPointer = MutableStateFlow<String?>(null)
    val pendingEndXPointer: StateFlow<String?> = _pendingEndXPointer

    private val _pendingAnnotationType = MutableStateFlow("note")
    val pendingAnnotationType: StateFlow<String> = _pendingAnnotationType

    private val _annotationNoteText = MutableStateFlow("")
    val annotationNoteText: StateFlow<String> = _annotationNoteText

    private val _annotationColorArgb = MutableStateFlow(DEFAULT_ANNOTATION_COLOR)
    val annotationColorArgb: StateFlow<Int> = _annotationColorArgb

    private val _editingAnnotationId = MutableStateFlow<Long?>(null)
    val editingAnnotationId: StateFlow<Long?> = _editingAnnotationId

    private val _tappedAnnotationId = MutableStateFlow<Long?>(null)
    val tappedAnnotationId: StateFlow<Long?> = _tappedAnnotationId

    // ── Text selection ───────────────────────────────────────────────────────

    private val _selectionActive = MutableStateFlow(false)
    val selectionActive: StateFlow<Boolean> = _selectionActive

    private val _showSelectionMenu = MutableStateFlow(false)
    val showSelectionMenu: StateFlow<Boolean> = _showSelectionMenu

    // Screen-pixel anchor of the current selection gesture (set on long-press).
    private var selectionStartX = 0
    private var selectionStartY = 0
    private var selectionEndX = 0
    private var selectionEndY = 0

    // Saved selection data from the initial long-press word snap (used as fallback).
    private var savedWordStartPos = ""
    private var savedWordEndPos = ""
    private var savedWordText = ""
    private var startSelectionJob: kotlinx.coroutines.Job? = null

    private val _selectionStartAnchor = MutableStateFlow<SelectionAnchor?>(null)
    val selectionStartAnchor: StateFlow<SelectionAnchor?> = _selectionStartAnchor

    private val _selectionEndAnchor = MutableStateFlow<SelectionAnchor?>(null)
    val selectionEndAnchor: StateFlow<SelectionAnchor?> = _selectionEndAnchor

    /** Called on long-press; anchors the selection at the pressed coordinates. */
    fun startTextSelection(x: Int, y: Int) {
        selectionStartX = x
        selectionStartY = y
        savedWordStartPos = ""
        savedWordEndPos = ""
        savedWordText = ""
        appLogger.debug("startTextSelection($x, $y)", tag = TAG)
        startSelectionJob = viewModelScope.launch(Dispatchers.IO) {
            val sel = Selection().apply {
                startX = x; startY = y; endX = x; endY = y
            }
            docView.updateSelection(sel)
            savedWordStartPos = sel.startPos
            savedWordEndPos = sel.endPos
            savedWordText = sel.text
            appLogger.debug(
                "startTextSelection job done: text='${sel.text}' startPos='${sel.startPos}' endPos='${sel.endPos}'",
                tag = TAG,
            )
            renderPage()
        }
    }

    /** Called while the finger drags after a long-press to extend the selection. */
    fun updateTextSelectionDrag(endX: Int, endY: Int) {
        selectionDragChannel.trySend(Pair(endX, endY))
    }

    /**
     * Called when the finger lifts after a selection drag. If the engine found
     * text, opens the selection menu; otherwise clears the selection.
     */
    fun finalizeTextSelection(endX: Int, endY: Int) {
        appLogger.debug(
            "finalizeTextSelection($endX, $endY) called, startX=$selectionStartX startY=$selectionStartY",
            tag = TAG,
        )
        viewModelScope.launch(Dispatchers.IO) {
            startSelectionJob?.join()

            appLogger.debug(
                "finalizeTextSelection after join: savedWordText='$savedWordText' savedStart='$savedWordStartPos' savedEnd='$savedWordEndPos'",
                tag = TAG,
            )

            val isWordTap = endX == selectionStartX && endY == selectionStartY
            val text: String
            val xpointer: String
            val endXpointer: String

            if (isWordTap) {
                // Word-tap: the engine already selected the word in startTextSelection.
                // Calling updateSelection again with the same point is redundant and can
                // produce an empty range if the engine re-evaluates the point differently.
                // Use the saved word data directly.
                text = savedWordText
                xpointer = savedWordStartPos
                endXpointer = savedWordEndPos
                appLogger.debug("finalizeTextSelection: word-tap path, text='$text'", tag = TAG)
            } else {
                val sel = Selection().apply {
                    startX = selectionStartX; startY = selectionStartY
                    this.endX = endX; this.endY = endY
                }
                docView.updateSelection(sel)
                text = sel.text.takeIf { it.isNotBlank() } ?: savedWordText
                xpointer = sel.startPos.takeIf { it.isNotBlank() } ?: savedWordStartPos
                endXpointer = sel.endPos.takeIf { it.isNotBlank() } ?: savedWordEndPos
                appLogger.debug(
                    "finalizeTextSelection: drag path, selText='${sel.text}' finalText='$text'",
                    tag = TAG,
                )
            }

            if (text.isNotBlank()) {
                selectionEndX = endX
                selectionEndY = endY
                _pendingSelectedText.value = text
                _pendingXPointer.value = xpointer.takeIf { it.isNotBlank() }
                _pendingEndXPointer.value = endXpointer.takeIf { it.isNotBlank() }
                updateHandleAnchors(xpointer, endXpointer, endX.toFloat(), endY.toFloat())
                appLogger.debug(
                    "finalizeTextSelection: setting showSelectionMenu=true, startAnchor=${_selectionStartAnchor.value} endAnchor=${_selectionEndAnchor.value}",
                    tag = TAG,
                )
                _selectionActive.value = true
                _showSelectionMenu.value = true
            } else {
                appLogger.debug("finalizeTextSelection: text blank, clearing selection", tag = TAG)
                docView.clearSelection()
                renderPage()
            }
        }
    }

    /** Cancels any in-progress text selection. */
    fun cancelTextSelection() {
        _selectionActive.value = false
        _selectionStartAnchor.value = null
        _selectionEndAnchor.value = null
        viewModelScope.launch(Dispatchers.IO) {
            docView.clearSelection()
            renderPage()
        }
    }

    fun dismissSelectionMenu() {
        _showSelectionMenu.value = false
        _selectionActive.value = false
        _selectionStartAnchor.value = null
        _selectionEndAnchor.value = null
        _pendingSelectedText.value = null
        _pendingXPointer.value = null
        _pendingEndXPointer.value = null
        viewModelScope.launch(Dispatchers.IO) {
            docView.clearSelection()
            renderPage()
        }
    }

    fun onSelectionMenuHighlight() {
        _showSelectionMenu.value = false
        _selectionActive.value = false
        startHighlightColorPicker(_pendingSelectedText.value, _pendingXPointer.value)
    }

    fun onSelectionMenuNote() {
        _showSelectionMenu.value = false
        _selectionActive.value = false
        startAnnotation("note", _pendingSelectedText.value, _pendingXPointer.value)
    }

    fun moveSelectionStartHandle(x: Int, y: Int) {
        startHandleChannel.trySend(Pair(x, y))
    }

    fun moveSelectionEndHandle(x: Int, y: Int) {
        endHandleChannel.trySend(Pair(x, y))
    }

    private fun updateHandleAnchors(
        startPos: String,
        endPos: String,
        fallbackX: Float = -1f,
        fallbackY: Float = -1f,
    ) {
        val rects = if (startPos.isNotBlank() && endPos.isNotBlank()) {
            docView.getXPointerRects(startPos, endPos)
        } else null

        appLogger.debug(
            "updateHandleAnchors: startPos='$startPos' endPos='$endPos' rects=${rects?.size} fallbackX=$fallbackX fallbackY=$fallbackY",
            tag = TAG,
        )

        if (rects != null && rects.size >= 4) {
            _selectionStartAnchor.value = SelectionAnchor(rects[0].toFloat(), rects[3].toFloat())
            val last = rects.size - 4
            _selectionEndAnchor.value =
                SelectionAnchor(rects[last + 2].toFloat(), rects[last + 3].toFloat())
            appLogger.debug(
                "updateHandleAnchors: from rects start=${_selectionStartAnchor.value} end=${_selectionEndAnchor.value}",
                tag = TAG,
            )
        } else if (fallbackX >= 0f) {
            _selectionStartAnchor.value = SelectionAnchor(fallbackX - 20f, fallbackY)
            _selectionEndAnchor.value = SelectionAnchor(fallbackX + 20f, fallbackY)
            appLogger.debug(
                "updateHandleAnchors: from fallback start=${_selectionStartAnchor.value} end=${_selectionEndAnchor.value}",
                tag = TAG,
            )
        } else {
            appLogger.debug("updateHandleAnchors: NO anchors set (no rects and no fallback)", tag = TAG)
        }
    }

    fun finalizeHandleDrag() {
        val xp = _pendingXPointer.value ?: return
        val endXp = _pendingEndXPointer.value ?: return
        viewModelScope.launch(Dispatchers.IO) { updateHandleAnchors(xp, endXp) }
    }

    // Coalesces rapid scroll deltas so only one render fires per batch of touch events.
    private val scrollChannel = Channel<Int>(Channel.UNLIMITED)

    // Conflated: only the latest drag position is kept; stale intermediates are dropped.
    private val selectionDragChannel = Channel<Pair<Int, Int>>(Channel.CONFLATED)

    // Handle drag channels — conflated so only the latest position renders per frame.
    private val startHandleChannel = Channel<Pair<Int, Int>>(Channel.CONFLATED)
    private val endHandleChannel = Channel<Pair<Int, Int>>(Channel.CONFLATED)

    // ── Initialization ───────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            launch {
                userPreferencesRepository.readerTheme.collectLatest {
                    _themeId.value = it; scheduleApply()
                }
            }
            launch {
                userPreferencesRepository.fontSize.collectLatest {
                    _fontSize.value = it.toDouble(); scheduleApply()
                }
            }
            launch {
                userPreferencesRepository.pageScroll.collectLatest {
                    _pageScroll.value = it; scheduleApply()
                }
            }
            launch {
                userPreferencesRepository.customizeLayout.collectLatest {
                    _customizeLayout.value = it; scheduleApply()
                }
            }
            launch {
                userPreferencesRepository.lineSpacing.collectLatest {
                    _lineSpacing.value = it.toDouble(); scheduleApply()
                }
            }
            launch {
                userPreferencesRepository.charSpacing.collectLatest {
                    _characterSpacing.value = it.toDouble(); scheduleApply()
                }
            }
            launch {
                userPreferencesRepository.wordSpacing.collectLatest {
                    _wordSpacing.value = it.toDouble(); scheduleApply()
                }
            }
            launch {
                userPreferencesRepository.margins.collectLatest {
                    _margins.value = it.toDouble(); scheduleApply()
                }
            }
            launch {
                userPreferencesRepository.columnCount.collectLatest {
                    _columnCount.value = it; scheduleApply()
                }
            }
            launch {
                userPreferencesRepository.justifyText.collectLatest {
                    _justifyText.value = it; scheduleApply()
                }
            }
        }
        // Debounced settings apply: waits 300ms after last change before hitting the engine.
        viewModelScope.launch(Dispatchers.IO) {
            _settingsTrigger.debounce(300L).collect {
                if (documentLoaded) {
                    applyCurrentSettings()
                    renderPage()
                }
            }
        }

        docView.create()

        // Drain scroll channel: coalesce pending deltas into one engine call + render.
        viewModelScope.launch(Dispatchers.IO) {
            for (delta in scrollChannel) {
                var total = delta
                while (true) total += scrollChannel.tryReceive().getOrNull() ?: break
                docView.doCommand(ReaderCommand.DCMD_SCROLL_BY, total)
                renderPage()
            }
        }

        // Drain selection drag channel: only the latest position matters per frame.
        viewModelScope.launch(Dispatchers.IO) {
            for ((endX, endY) in selectionDragChannel) {
                val sel = Selection().apply {
                    startX = selectionStartX; startY = selectionStartY
                    this.endX = endX; this.endY = endY
                }
                docView.updateSelection(sel)
                renderPage()
            }
        }

        // Drain start handle drag channel.
        viewModelScope.launch(Dispatchers.IO) {
            for ((x, y) in startHandleChannel) {
                val prevStartX = selectionStartX
                val prevStartY = selectionStartY
                val sel = Selection().apply {
                    startX = x; startY = y; endX = selectionEndX; endY = selectionEndY
                }
                docView.updateSelection(sel)
                if (!sel.text.isNullOrBlank()) {
                    selectionStartX = x
                    selectionStartY = y
                    _pendingSelectedText.value = sel.text
                    _pendingXPointer.value = sel.startPos.takeIf { it.isNotBlank() }
                    _pendingEndXPointer.value = sel.endPos.takeIf { it.isNotBlank() }
                } else {
                    // Engine found no text at this position (e.g. inter-line gap); keep previous selection.
                    val restore = Selection().apply {
                        startX = prevStartX; startY = prevStartY
                        endX = selectionEndX; endY = selectionEndY
                    }
                    docView.updateSelection(restore)
                }
                renderPage()
            }
        }

        // Drain end handle drag channel.
        viewModelScope.launch(Dispatchers.IO) {
            for ((x, y) in endHandleChannel) {
                val prevEndX = selectionEndX
                val prevEndY = selectionEndY
                val sel = Selection().apply {
                    startX = selectionStartX; startY = selectionStartY; endX = x; endY = y
                }
                docView.updateSelection(sel)
                if (!sel.text.isNullOrBlank()) {
                    selectionEndX = x
                    selectionEndY = y
                    _pendingSelectedText.value = sel.text
                    _pendingXPointer.value = sel.startPos.takeIf { it.isNotBlank() }
                    _pendingEndXPointer.value = sel.endPos.takeIf { it.isNotBlank() }
                } else {
                    // Engine found no text at this position (e.g. inter-line gap); keep previous selection.
                    val restore = Selection().apply {
                        startX = selectionStartX; startY = selectionStartY
                        endX = prevEndX; endY = prevEndY
                    }
                    docView.updateSelection(restore)
                }
                renderPage()
            }
        }
    }

    // ── Open document ────────────────────────────────────────────────────────

    /**
     * Opens [bookUri] in the engine. Must be called after the first [onViewReady]
     * so we have valid dimensions. Can also restore [initialXPointer] position.
     */
    fun openBook(context: Context, initialXPointer: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val (data, contentPath) = readUriToBytes(context, bookUri) ?: return@launch
            val ok = docView.loadDocumentFromBuffer(data, contentPath)
            if (!ok) return@launch

            applyCurrentSettings()
            documentLoaded = true

            if (!initialXPointer.isNullOrBlank()) {
                docView.goToPosition(initialXPointer, precise = false)
            }

            loadTOC()
            applyHighlightsInternal(latestAnnotations)
        }
    }

    // ── View size callback ───────────────────────────────────────────────────

    fun onViewReady(width: Int, height: Int) {
        if (width == viewWidth && height == viewHeight) return
        viewWidth = width
        viewHeight = height
        viewModelScope.launch(Dispatchers.IO) {
            docView.resize(width, height)
            renderPage()
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    fun nextPage() = navigate(ReaderCommand.DCMD_PAGEDOWN)
    fun prevPage() = navigate(ReaderCommand.DCMD_PAGEUP)
    fun nextChapter() = navigate(ReaderCommand.DCMD_MOVE_BY_CHAPTER, 1)
    fun prevChapter() = navigate(ReaderCommand.DCMD_MOVE_BY_CHAPTER, -1)

    fun scrollBy(pixels: Int) {
        scrollChannel.trySend(pixels)
    }

    fun goToXPointer(xpointer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            docView.goToPosition(xpointer)
            renderPage()
        }
    }

    fun goToProgress(fraction: Float) {
        val props = _positionProps.value ?: return
        if (props.fullHeight <= props.pageHeight) return
        val targetY = ((props.fullHeight - props.pageHeight) * fraction).toInt()
        viewModelScope.launch(Dispatchers.IO) {
            docView.doCommand(ReaderCommand.DCMD_GO_POS, targetY)
            renderPage()
        }
    }

    /** Returns an xpointer representing the current reading position. */
    fun currentXPointer(): String? =
        docView.getCurrentPageBookmark()?.startPos

    /** Re-renders the current page without changing position. Call after settings change. */
    fun redraw() {
        viewModelScope.launch(Dispatchers.IO) { renderPage() }
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    // Engine only accepts margins from this exact list; any other value resets to the default (8).
    private val MARGIN_OPTIONS = intArrayOf(
        0,
        1,
        2,
        3,
        4,
        5,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15,
        16,
        20,
        25,
        30,
        40,
        50,
        60,
        80,
        100,
        130,
        150,
        200,
        300
    )

    private fun snapMargin(px: Int) = MARGIN_OPTIONS.minByOrNull { kotlin.math.abs(it - px) } ?: px

    fun applyCurrentSettings() {
        val dm = getApplication<Application>().resources.displayMetrics
        // crengine.font.size takes screen pixels. Scale like Android sp → px:
        // 16sp base at scale 1.0 gives 48px at xxhdpi (density=3).
        val fontSizePx = (16.0 * dm.density * _fontSize.value).toInt().coerceIn(10, 120)

        val theme = findReaderTheme(_themeId.value)
        val customize = _customizeLayout.value
        val props = Properties().apply {
            setProperty("crengine.render.dpi", dm.densityDpi.toString())
            setProperty(
                "background.color.default",
                "#%06X".format(theme.background.toArgb() and 0xFFFFFF)
            )
            setProperty(
                "font.color.default",
                "#%06X".format(theme.textColor.toArgb() and 0xFFFFFF)
            )
            setProperty("crengine.font.size", fontSizePx.toString())

            // Page/scroll view mode: "crengine.page.view.mode" — 1=pages, 0=scroll
            setProperty("crengine.page.view.mode", if (_pageScroll.value) "0" else "1")

            // Line spacing (integer percent; clamped to 200 which is the engine's maximum)
            val lineSpacePct =
                if (customize) (_lineSpacing.value * 100).toInt().coerceIn(80, 200) else 100
            setProperty("crengine.interline.space", lineSpacePct.toString())

            // Word spacing: scales width of space characters (100 = normal)
            // slider range is -75..400; engine range 10..500
            val wordSpacePct = if (customize)
                (100 + _wordSpacing.value.toInt()).coerceIn(10, 500)
            else 100
            setProperty("crengine.style.space.width.scale.percent", wordSpacePct.toString())

            // Character spacing: CSS letter-spacing applied globally via stylesheet macro.
            // crengine.style.max.added.letter.spacing.percent only works during justification;
            // styles.def.letter-spacing is a real CSS property that always takes effect.
            val letterSpacingEm = if (customize && _characterSpacing.value > 0)
                "letter-spacing: ${"%.3f".format(_characterSpacing.value * 0.01)}em"
            else
                "letter-spacing: normal"
            setProperty("styles.def.letter-spacing", letterSpacingEm)

            // Text justification via document style rule (txt.def.format is TXT-only)
            setProperty(
                "styles.def.align",
                if (customize && _justifyText.value) "text-align: justify" else "text-align: left"
            )

            // Margins: slider is dp → convert to px → snap to engine's allowed list.
            val marginsValue = if (customize) _margins.value else DEFAULT_MARGINS.toDouble()
            val marginPx = snapMargin((marginsValue * dm.density).toInt()).toString()
            setProperty("crengine.page.margin.left", marginPx)
            setProperty("crengine.page.margin.right", marginPx)
            setProperty("crengine.page.margin.top", marginPx)
            setProperty("crengine.page.margin.bottom", marginPx)

            // Native bookmark highlights disabled; we draw custom-colored overlays in Compose.
            setProperty("crengine.highlight.bookmarks", "0")

            // Disable the native crengine status bar (author/title/clock/page-number header).
            setProperty("window.status.line", "0")
        }
        docView.applySettings(props)
    }

    // ── Highlights ───────────────────────────────────────────────────────────

    fun applyHighlights(annotations: List<Annotation>) {
        latestAnnotations = annotations
        if (!documentLoaded) return
        viewModelScope.launch(Dispatchers.IO) { applyHighlightsInternal(annotations) }
    }

    private suspend fun applyHighlightsInternal(annotations: List<Annotation>) {
        // Disable crengine's native 2-color rendering; we draw custom-colored overlays ourselves.
        docView.hilightBookmarks(emptyArray())
        renderPage()
        updateHighlightRects(annotations)
    }

    private fun updateHighlightRects(annotations: List<Annotation> = latestAnnotations) {
        val rects = mutableListOf<HighlightRect>()
        for (ann in annotations) {
            if (ann.type != AnnotationType.HIGHLIGHT && ann.type != AnnotationType.NOTE) continue
            val start = ann.locator
            val end = ann.endLocator.ifBlank { ann.locator }
            val raw = docView.getXPointerRects(start, end) ?: continue
            var i = 0
            while (i + 3 < raw.size) {
                rects.add(
                    HighlightRect(
                        raw[i],
                        raw[i + 1],
                        raw[i + 2],
                        raw[i + 3],
                        ann.color,
                        ann.id
                    )
                )
                i += 4
            }
        }
        _highlightRects.value = rects
    }

    // ── TOC ──────────────────────────────────────────────────────────────────

    private fun loadTOC() {
        val root = docView.getTOC()
        _tocItems.value = root.flatten()
    }

    // ── Annotation creation ──────────────────────────────────────────────────

    fun startHighlightColorPicker(text: String?, xpointer: String?) {
        _pendingSelectedText.value = text
        _pendingXPointer.value = xpointer
        _showHighlightColorPicker.value = true
    }

    fun dismissHighlightColorPicker() {
        _showHighlightColorPicker.value = false
        _pendingSelectedText.value = null
        _pendingXPointer.value = null
        _pendingEndXPointer.value = null
    }

    fun startAnnotation(
        type: String = "note",
        prefilledText: String? = null,
        xpointer: String? = null
    ) {
        _pendingAnnotationType.value = type
        _pendingSelectedText.value = prefilledText
        _pendingXPointer.value = xpointer
        _annotationNoteText.value = ""
        _annotationColorArgb.value = DEFAULT_ANNOTATION_COLOR
        _showAnnotationCreator.value = true
    }

    fun setAnnotationNote(text: String) {
        _annotationNoteText.value = text
    }

    fun setAnnotationColor(argb: Int) {
        _annotationColorArgb.value = argb
    }

    fun cancelAnnotation() {
        _showAnnotationCreator.value = false
        _editingAnnotationId.value = null
        _pendingSelectedText.value = null
        _pendingXPointer.value = null
        _pendingEndXPointer.value = null
        _pendingAnnotationType.value = "note"
        _annotationNoteText.value = ""
        _annotationColorArgb.value = DEFAULT_ANNOTATION_COLOR
    }

    fun onAnnotationTapped(id: Long) {
        _tappedAnnotationId.value = id
    }

    fun dismissAnnotationMenu() {
        _tappedAnnotationId.value = null
    }

    fun findAnnotationAtPoint(x: Float, y: Float): Long? =
        _highlightRects.value.lastOrNull { rect ->
            x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom
        }?.annotationId

    // ── Fullscreen image viewer ──────────────────────────────────────────────

    private val _fullscreenImage = MutableStateFlow<Bitmap?>(null)
    val fullscreenImage: StateFlow<Bitmap?> = _fullscreenImage

    // Bitmap decoded on IO as soon as the finger goes down; consumed (or cancelled)
    // when the long-press fires 500 ms later — avoids calling suspend fns from a
    // @RestrictsSuspension pointer-input scope.
    private var pendingImageJob: kotlinx.coroutines.Job? = null
    private val pendingImageBitmap = MutableStateFlow<Bitmap?>(null)

    /** Called at finger-down. Starts a background decode of any image under (x,y). */
    fun preloadImageAtPoint(x: Int, y: Int) {
        pendingImageJob?.cancel()
        pendingImageBitmap.value = null
        pendingImageJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val imageInfo = org.coolreader.crengine.ImageInfo().apply {
                    // bufWidth/bufHeight are inputs: crengine uses them to decide whether
                    // to auto-rotate the image (portrait vs landscape screen orientation).
                    bufWidth = viewWidth
                    bufHeight = viewHeight
                }
                if (!docView.checkImage(x, y, imageInfo)) return@launch
                // scaledWidth/scaledHeight are the outputs: the final decoded dimensions.
                val w = imageInfo.scaledWidth
                val h = imageInfo.scaledHeight
                if (w <= 0 || h <= 0) return@launch
                val bmp = createBitmap(w, h)
                if (docView.drawImage(bmp, imageInfo)) {
                    docView.closeImage()
                    pendingImageBitmap.value = bmp
                } else {
                    docView.closeImage()
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Called when a long-press fires. If a decoded bitmap is ready, promotes it
     * to the fullscreen viewer and returns true. Returns false if no image was
     * found so the caller can fall back to text selection.
     */
    fun consumePendingImage(): Boolean {
        val bmp = pendingImageBitmap.value ?: return false
        pendingImageBitmap.value = null
        pendingImageJob = null
        _fullscreenImage.value = bmp
        return true
    }

    /** Called when a gesture is resolved as something other than a long press. */
    fun cancelImagePreload() {
        pendingImageJob?.cancel()
        pendingImageJob = null
        pendingImageBitmap.value = null
    }

    fun dismissFullscreenImage() {
        _fullscreenImage.value = null
    }

    fun startAnnotationEdit(annotation: Annotation) {
        _tappedAnnotationId.value = null
        _editingAnnotationId.value = annotation.id
        _pendingAnnotationType.value = annotation.type.name.lowercase()
        _pendingSelectedText.value = annotation.text
        _annotationNoteText.value = annotation.note ?: ""
        _annotationColorArgb.value = annotation.color
        _showAnnotationCreator.value = true
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun navigate(cmd: Int, param: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            docView.doCommand(cmd, param)
            renderPage()
        }
    }

    private suspend fun renderPage() = withContext(Dispatchers.IO) {
        if (viewWidth <= 0 || viewHeight <= 0) return@withContext

        val bmp = currentBitmap?.takeIf {
            it.width == viewWidth && it.height == viewHeight
        } ?: createBitmap(viewWidth, viewHeight).also {
            currentBitmap = it
        }

        docView.getPageImage(bmp)

        val props = docView.getPositionProps(precise = false)
        _positionProps.value = props
        _progress.value = props?.progressFraction() ?: 0f
        _pageBitmap.value = bmp.copy(Bitmap.Config.ARGB_8888, false)

        // Recompute overlay rects for the new view position, but skip during
        // selection drag (renderPage is called per-frame then, so this would thrash).
        if (latestAnnotations.isNotEmpty() && !_selectionActive.value) {
            updateHighlightRects()
        } else if (latestAnnotations.isEmpty()) {
            _highlightRects.value = emptyList()
        }
    }

    /**
     * Reads the content URI into memory and returns (data, displayName).
     * CREngine uses [displayName] (with its extension) to detect the document format.
     */
    private suspend fun readUriToBytes(context: Context, uri: Uri): Pair<ByteArray, String>? =
        withContext(Dispatchers.IO) {
            runCatching {
                // Query for the display name so CREngine gets the correct file extension.
                val displayName = context.contentResolver
                    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                    ?: uri.lastPathSegment
                    ?: "book.epub"

                val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@runCatching null

                Pair(data, displayName)
            }.getOrNull()
        }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        scrollChannel.close()
        selectionDragChannel.close()
        docView.destroy()
        currentBitmap?.recycle()
        currentBitmap = null
    }

    // ── ViewModel factory ────────────────────────────────────────────────────

    class Factory(
        private val application: Application,
        private val bookUri: Uri,
        private val bookTitle: String,
        private val bookId: String,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val appLogger: AppLogger,
    ) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderViewModel(
                    application, bookUri, bookTitle, bookId, userPreferencesRepository, appLogger
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

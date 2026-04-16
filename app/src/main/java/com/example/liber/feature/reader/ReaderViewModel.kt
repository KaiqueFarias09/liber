package com.example.liber.feature.reader

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.AnnotationType
import com.example.liber.data.repository.UserPreferencesRepository
import com.example.liber.feature.reader.engine.ReaderCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.coolreader.crengine.Bookmark
import org.coolreader.crengine.DocView
import org.coolreader.crengine.PositionProperties
import org.coolreader.crengine.TOCItem
import java.util.Properties

// Default annotation colour — yellow at 50 % opacity
private val DEFAULT_ANNOTATION_COLOR = 0x80FFD60A.toInt()

private const val DEFAULT_LINE_SPACING = 1.4f
private const val DEFAULT_CHAR_SPACING = 0.0f
private const val DEFAULT_WORD_SPACING = 0.0f
private const val DEFAULT_MARGINS = 0.0f

class ReaderViewModel(
    application: Application,
    val bookUri: Uri,
    val bookTitle: String,
    val bookId: String,
    private val userPreferencesRepository: UserPreferencesRepository,
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

    // ── View dimensions ──────────────────────────────────────────────────────

    private var viewWidth = 0
    private var viewHeight = 0
    private var currentBitmap: Bitmap? = null

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

    // ── Initialization ───────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            launch { userPreferencesRepository.readerTheme.collectLatest { _themeId.value = it } }
            launch {
                userPreferencesRepository.fontSize.collectLatest {
                    _fontSize.value = it.toDouble()
                }
            }
            launch { userPreferencesRepository.pageScroll.collectLatest { _pageScroll.value = it } }
            launch {
                userPreferencesRepository.customizeLayout.collectLatest {
                    _customizeLayout.value = it
                }
            }
            launch {
                userPreferencesRepository.lineSpacing.collectLatest {
                    _lineSpacing.value = it.toDouble()
                }
            }
            launch {
                userPreferencesRepository.charSpacing.collectLatest {
                    _characterSpacing.value = it.toDouble()
                }
            }
            launch {
                userPreferencesRepository.wordSpacing.collectLatest {
                    _wordSpacing.value = it.toDouble()
                }
            }
            launch {
                userPreferencesRepository.margins.collectLatest {
                    _margins.value = it.toDouble()
                }
            }
            launch {
                userPreferencesRepository.columnCount.collectLatest {
                    _columnCount.value = it
                }
            }
            launch {
                userPreferencesRepository.justifyText.collectLatest {
                    _justifyText.value = it
                }
            }
        }
        docView.create()
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

            if (!initialXPointer.isNullOrBlank()) {
                docView.goToPosition(initialXPointer, precise = false)
            }

            loadTOC()
            renderPage()
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
        viewModelScope.launch(Dispatchers.IO) {
            docView.doCommand(ReaderCommand.DCMD_SCROLL_BY, pixels)
            renderPage()
        }
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
            docView.doCommand(ReaderCommand.DCMD_GO_SCROLL_POS, targetY)
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

    fun applyCurrentSettings() {
        val dm = getApplication<Application>().resources.displayMetrics
        // lvrend.cpp uses 96 as its base DPI, not 160. Scale accordingly.
        val renderDpi = (96 * dm.density).toInt()
        // crengine.font.size is in physical pixels — renderDpi does NOT scale it.
        // Convert: px = pt × densityDpi / 72  (12pt baseline, same as reference CoolReader)
        val fontSizePx = (12.0 * _fontSize.value * dm.densityDpi / 72.0).toInt().coerceIn(16, 256)

        val theme = findReaderTheme(_themeId.value)
        val props = Properties().apply {
            setProperty("crengine.render.dpi", renderDpi.toString())
            // Background and text colours
            setProperty(
                "crengine.background.color",
                String.format("%06X", theme.background.value.toInt() and 0xFFFFFF)
            )
            setProperty(
                "crengine.foreground.color",
                String.format("%06X", theme.textColor.value.toInt() and 0xFFFFFF)
            )
            setProperty("crengine.font.size", fontSizePx.toString())
            // Scroll vs page mode
            setProperty(
                "crengine.render.scroll.mode",
                if (_pageScroll.value) "1" else "0"
            )
            // Line spacing (crengine uses percent e.g. 140 = 1.4×)
            if (_customizeLayout.value) {
                setProperty(
                    "crengine.interline.space",
                    (_lineSpacing.value * 100).toInt().toString()
                )
            }
            // Text alignment
            if (_justifyText.value) {
                setProperty("crengine.txt.def.format", "1")
            }
        }
        docView.applySettings(props)
    }

    // ── Highlights ───────────────────────────────────────────────────────────

    fun applyHighlights(annotations: List<Annotation>) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookmarks = annotations
                .filter { it.type == AnnotationType.HIGHLIGHT || it.type == AnnotationType.NOTE }
                .map { ann ->
                    Bookmark().apply {
                        startPos = ann.locator
                        endPos = ann.locator
                        type = Bookmark.TYPE_COMMENT
                    }
                }
                .toTypedArray()
            docView.hilightBookmarks(bookmarks)
            renderPage()
        }
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
        } ?: Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888).also {
            currentBitmap = it
        }

        docView.getPageImage(bmp)

        val props = docView.getPositionProps(precise = false)
        _positionProps.value = props
        _progress.value = props?.progressFraction() ?: 0f
        _pageBitmap.value = bmp.copy(Bitmap.Config.ARGB_8888, false)
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
    ) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderViewModel(
                    application, bookUri, bookTitle, bookId, userPreferencesRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

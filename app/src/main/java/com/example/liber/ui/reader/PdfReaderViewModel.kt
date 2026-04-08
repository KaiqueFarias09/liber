package com.example.liber.ui.reader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.liber.data.AppDatabase
import com.example.liber.data.InkStrokeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class PdfInkTool { PEN, HIGHLIGHTER, ERASER }

data class PdfInkConfig(
    val tool: PdfInkTool = PdfInkTool.PEN,
    val color: Int = 0xFF2C2C2E.toInt(),
    val thickness: Float = 5f
)

/**
 * Holds the state for a pending PDF text-selection highlight (before the user picks a color).
 * [text] is the selected text (null if extraction failed), [page] is the 0-indexed page.
 */
data class PdfPendingHighlight(val text: String?, val page: Int)

class PdfReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    // ── UI chrome visibility ─────────────────────────────────────────────────
    private val _showUI = MutableStateFlow(true)
    val showUI: StateFlow<Boolean> = _showUI.asStateFlow()

    fun toggleUI() {
        _showUI.value = !_showUI.value
    }

    fun showUI() {
        _showUI.value = true
    }

    // ── Reading progress ─────────────────────────────────────────────────────
    private val _currentPage = MutableStateFlow(0)   // 0-indexed
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    fun onPageChanged(page: Int) {
        _currentPage.value = page
    }

    fun onDocumentLoaded(count: Int) {
        _totalPages.value = count
    }

    // ── Ink / annotation config ──────────────────────────────────────────────
    private val _inkConfig = MutableStateFlow(PdfInkConfig())
    val inkConfig: StateFlow<PdfInkConfig> = _inkConfig.asStateFlow()

    private val _drawingModeActive = MutableStateFlow(false)
    val drawingModeActive: StateFlow<Boolean> = _drawingModeActive.asStateFlow()

    fun setInkTool(tool: PdfInkTool) {
        _inkConfig.value = _inkConfig.value.copy(tool = tool)
    }

    fun setInkColor(argb: Int) {
        _inkConfig.value = _inkConfig.value.copy(color = argb)
    }

    fun setInkThickness(t: Float) {
        _inkConfig.value = _inkConfig.value.copy(thickness = t)
    }

    fun toggleDrawingMode() {
        _drawingModeActive.value = !_drawingModeActive.value
    }

    fun exitDrawingMode() {
        _drawingModeActive.value = false
    }

    // ── Note creator ─────────────────────────────────────────────────────────
    private val _showNoteCreator = MutableStateFlow(false)
    val showNoteCreator: StateFlow<Boolean> = _showNoteCreator.asStateFlow()

    private val _pendingNoteText = MutableStateFlow("")
    val pendingNoteText: StateFlow<String> = _pendingNoteText.asStateFlow()

    /** Text pre-filled from a text selection (the "quoted" excerpt). */
    private val _pendingNoteSelectedText = MutableStateFlow<String?>(null)
    val pendingNoteSelectedText: StateFlow<String?> = _pendingNoteSelectedText.asStateFlow()

    fun openNoteCreator() {
        _pendingNoteSelectedText.value = null
        _showNoteCreator.value = true
    }

    /** Opens the note creator pre-filled with text selected in the PDF. */
    fun openNoteCreatorWithSelection(selectedText: String?) {
        _pendingNoteSelectedText.value = selectedText?.takeIf { it.isNotBlank() }
        _showNoteCreator.value = true
    }

    fun setNoteText(text: String) {
        _pendingNoteText.value = text
    }

    fun dismissNoteCreator() {
        _showNoteCreator.value = false
        _pendingNoteText.value = ""
        _pendingNoteSelectedText.value = null
    }

    // ── Highlight (text selection) flow ──────────────────────────────────────
    private val _pendingHighlight = MutableStateFlow<PdfPendingHighlight?>(null)
    val pendingHighlight: StateFlow<PdfPendingHighlight?> = _pendingHighlight.asStateFlow()

    private val _highlightColor = MutableStateFlow(AnnotationColorOptions[0])
    val highlightColor: StateFlow<Int> = _highlightColor.asStateFlow()

    fun startHighlight(text: String?, page: Int) {
        _highlightColor.value = AnnotationColorOptions[0]
        _pendingHighlight.value = PdfPendingHighlight(text, page)
    }

    fun setHighlightColor(colorArgb: Int) {
        _highlightColor.value = colorArgb
    }

    fun dismissHighlight() {
        _pendingHighlight.value = null
    }

    // ── Writable URI (internal-storage copy for EditablePdfViewerFragment) ───
    private val _writableUri = MutableStateFlow<Uri?>(null)
    val writableUri: StateFlow<Uri?> = _writableUri.asStateFlow()

    // ── Ink stroke persistence ────────────────────────────────────────────────
    private val _persistedStrokes = MutableStateFlow<List<InkStrokeEntity>>(emptyList())
    val persistedStrokes: StateFlow<List<InkStrokeEntity>> = _persistedStrokes.asStateFlow()

    fun loadStrokes(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _persistedStrokes.value = db.inkStrokeDao().getForBook(bookId)
        }
    }

    fun saveStroke(entity: InkStrokeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.inkStrokeDao().insert(entity)
        }
    }

    /**
     * Copies the source PDF to internal storage so the PDF viewer fragment can
     * write annotations back into the file. Skips the copy if the local copy
     * already exists (preserving previously saved annotations).
     */
    fun prepareWritableUri(sourceUri: Uri, bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val destFile = File(app.filesDir, "pdf_work_$bookId.pdf")
            if (!destFile.exists()) {
                app.contentResolver.openInputStream(sourceUri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            _writableUri.value = Uri.fromFile(destFile)
        }
    }
}

package com.example.liber.ui.reader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

class PdfReaderViewModel(application: Application) : AndroidViewModel(application) {

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

    fun openNoteCreator() {
        _showNoteCreator.value = true
    }

    fun setNoteText(text: String) {
        _pendingNoteText.value = text
    }

    fun dismissNoteCreator() {
        _showNoteCreator.value = false
        _pendingNoteText.value = ""
    }

    // ── Writable URI (internal-storage copy for EditablePdfViewerFragment) ───
    private val _writableUri = MutableStateFlow<Uri?>(null)
    val writableUri: StateFlow<Uri?> = _writableUri.asStateFlow()

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

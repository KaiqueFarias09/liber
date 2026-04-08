@file:SuppressLint("NewApi")

package com.example.liber.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.net.Uri
import android.util.SparseArray
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.PdfDocument
import androidx.pdf.view.PdfView

/**
 * Actions that can be triggered from text selected inside the PDF viewer.
 */
enum class PdfTextAction { HIGHLIGHT, NOTE, SHARE }

/**
 * Thin subclass of [PdfViewerFragment] that bridges page-change and document-load
 * events back to Compose via simple lambda callbacks, and intercepts text selection
 * to expose Highlight, Add Note, and Share actions.
 *
 * Named class (not anonymous) because FragmentManager requires a no-arg constructor.
 */
class LibPdfViewerFragment : androidx.pdf.viewer.fragment.PdfViewerFragment() {

    /** Invoked once the document finishes loading; receives total page count. */
    var onDocumentLoaded: ((pageCount: Int) -> Unit)? = null

    /** Invoked whenever the first visible page index (0-indexed) changes. */
    var onPageChanged: ((page: Int) -> Unit)? = null

    /**
     * Invoked on every viewport change with a map of visible page index → screen RectF.
     * Used by the drawing overlay to map stroke coordinates to page-relative space.
     */
    var onPageLocationsChanged: ((Map<Int, RectF>) -> Unit)? = null

    /**
     * Invoked when the user selects a text action (Highlight, Add Note, Share) from
     * the PDF's text selection menu. [text] is the selected text (null if extraction failed).
     * [page] is the 0-indexed page on which the selection occurred.
     */
    var onTextSelectionAction: ((action: PdfTextAction, text: String?, page: Int) -> Unit)? = null

    /**
     * Provides the current 0-indexed page number at the time of a text selection action.
     * The PDF screen should assign this to return [PdfReaderViewModel.currentPage.value].
     */
    var getCurrentPage: (() -> Int)? = null

    private var pendingDocumentUri: Uri? = null
    private var pdfViewRef: PdfView? = null
    private var viewportListener: PdfView.OnViewportChangedListener? = null
    private var windowCallbackWrapper: PdfSelectionWindowCallbackWrapper? = null

    /**
     * Sets [documentUri] if the fragment is already attached, otherwise queues it
     * to be applied in [onAttach]. This avoids crashes when Compose's update block
     * runs before the fragment transaction completes.
     */
    fun setDocumentUriWhenReady(uri: Uri) {
        if (isAdded) {
            documentUri = uri
        } else {
            pendingDocumentUri = uri
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        pendingDocumentUri?.let { uri ->
            documentUri = uri
            pendingDocumentUri = null
        }
    }

    override fun onStart() {
        super.onStart()
        val window = requireActivity().window
        // Only wrap if not already wrapped (e.g., fragment restart)
        if (window.callback !is PdfSelectionWindowCallbackWrapper) {
            val wrapper = PdfSelectionWindowCallbackWrapper(
                original = window.callback,
                onActionTriggered = { action, text ->
                    val page = getCurrentPage?.invoke() ?: 0
                    onTextSelectionAction?.invoke(action, text, page)
                },
                getContext = { requireContext() },
            )
            windowCallbackWrapper = wrapper
            window.callback = wrapper
        }
    }

    override fun onStop() {
        // Restore the original Window.Callback so we don't leak
        val window = activity?.window
        val current = window?.callback
        if (current === windowCallbackWrapper) {
            window?.callback = windowCallbackWrapper?.original
        }
        windowCallbackWrapper = null
        super.onStop()
    }

    @OptIn(ExperimentalPdfApi::class)
    override fun onPdfViewCreated(pdfView: PdfView) {
        super.onPdfViewCreated(pdfView)
        pdfViewRef = pdfView
        viewportListener = object : PdfView.OnViewportChangedListener {
            override fun onViewportChanged(
                firstVisiblePage: Int,
                visiblePagesCount: Int,
                pageLocations: SparseArray<RectF>,
                zoomLevel: Float,
            ) {
                onPageChanged?.invoke(firstVisiblePage)
                if (onPageLocationsChanged != null) {
                    val map = HashMap<Int, RectF>(pageLocations.size())
                    for (i in 0 until pageLocations.size()) {
                        map[pageLocations.keyAt(i)] =
                            RectF(pageLocations.valueAt(i))
                    }
                    onPageLocationsChanged?.invoke(map)
                }
            }
        }
        pdfView.addOnViewportChangedListener(viewportListener!!)
    }

    override fun onLoadDocumentSuccess(document: PdfDocument) {
        super.onLoadDocumentSuccess(document)
        onDocumentLoaded?.invoke(document.pageCount)
    }

    override fun onDestroyView() {
        viewportListener?.let { pdfViewRef?.removeOnViewportChangedListener(it) }
        pdfViewRef = null
        viewportListener = null
        super.onDestroyView()
    }

    /** Programmatically scrolls the PDF to the given 0-indexed page number. */
    fun scrollToPage(pageNum: Int) {
        // Post to the next frame: PdfView requires its PdfDocument to be fully
        // initialized before scrollToPage can succeed, and onLoadDocumentSuccess
        // fires before that initialization completes.
        pdfViewRef?.post { pdfViewRef?.scrollToPage(pageNum) }
    }
}

// ── Window.Callback wrapper ───────────────────────────────────────────────────

private const val MENU_ID_HIGHLIGHT = 0xBEEF_01
private const val MENU_ID_NOTE      = 0xBEEF_02
private const val MENU_ID_SHARE_PDF = 0xBEEF_03

/**
 * Wraps the Activity's [Window.Callback] to intercept ActionMode creation calls
 * originating from the PDF viewer's text selection. Injects "Highlight", "Add Note",
 * and "Share" items into the floating text selection menu.
 */
internal class PdfSelectionWindowCallbackWrapper(
    val original: Window.Callback,
    private val onActionTriggered: (PdfTextAction, String?) -> Unit,
    private val getContext: () -> Context,
) : Window.Callback by original {

    override fun onWindowStartingActionMode(callback: ActionMode.Callback, type: Int): ActionMode? {
        return original.onWindowStartingActionMode(
            PdfActionModeCallbackWrapper(callback, onActionTriggered, getContext),
            type,
        )
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback): ActionMode? {
        return original.onWindowStartingActionMode(
            PdfActionModeCallbackWrapper(callback, onActionTriggered, getContext),
        )
    }
}

/**
 * Wraps an [ActionMode.Callback] to add custom items (Highlight, Add Note, Share)
 * to the PDF text selection menu, and handle their clicks by extracting the selected
 * text via the clipboard and delegating to [onActionTriggered].
 */
private class PdfActionModeCallbackWrapper(
    private val original: ActionMode.Callback,
    private val onActionTriggered: (PdfTextAction, String?) -> Unit,
    private val getContext: () -> Context,
) : ActionMode.Callback {

    private var savedMenu: Menu? = null

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val result = original.onCreateActionMode(mode, menu)
        menu.add(Menu.NONE, MENU_ID_HIGHLIGHT, 1, "Highlight")
        menu.add(Menu.NONE, MENU_ID_NOTE,      2, "Add Note")
        menu.add(Menu.NONE, MENU_ID_SHARE_PDF, 3, "Share")
        savedMenu = menu
        return result
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        savedMenu = menu
        return original.onPrepareActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_ID_HIGHLIGHT, MENU_ID_NOTE, MENU_ID_SHARE_PDF -> {
                val action = when (item.itemId) {
                    MENU_ID_HIGHLIGHT -> PdfTextAction.HIGHLIGHT
                    MENU_ID_NOTE      -> PdfTextAction.NOTE
                    else              -> PdfTextAction.SHARE
                }

                // Trigger the system Copy action so the selected text lands on the clipboard,
                // then read it back synchronously (copy runs on the same main-thread call stack).
                val text = extractSelectedText(mode)

                if (action == PdfTextAction.SHARE) {
                    // Handle share inline rather than routing through the ViewModel
                    val shareText = text ?: ""
                    if (shareText.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        runCatching {
                            getContext().startActivity(Intent.createChooser(intent, null))
                        }
                    }
                    mode.finish()
                } else {
                    onActionTriggered(action, text)
                    mode.finish()
                }
                true
            }
            else -> original.onActionItemClicked(mode, item)
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        savedMenu = null
        original.onDestroyActionMode(mode)
    }

    /**
     * Attempts to extract the selected text by triggering the standard Copy action on
     * the original callback (which copies to clipboard synchronously on the main thread)
     * and immediately reading back the clipboard.
     *
     * Returns null if there is no copy item or the clipboard is empty.
     */
    private fun extractSelectedText(mode: ActionMode): String? {
        val ctx = runCatching { getContext() }.getOrNull() ?: return null
        val clipboard = ctx.getSystemService(android.content.ClipboardManager::class.java)
            ?: return null

        val copyItem = savedMenu?.findItem(android.R.id.copy) ?: return null
        // Calling onActionItemClicked with the copy item copies text to clipboard synchronously.
        runCatching { original.onActionItemClicked(mode, copyItem) }
        return clipboard.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString()
    }
}

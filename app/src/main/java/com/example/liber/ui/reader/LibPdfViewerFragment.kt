@file:SuppressLint("NewApi")

package com.example.liber.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.net.Uri
import android.util.SparseArray
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
 * The text-selection interception works by wrapping the view returned by
 * [onCreateView] in a [PdfSelectionInterceptorLayout] that overrides
 * [ViewGroup.startActionModeForChild]. This ensures our callback wrapper is
 * in place before [android.view.PhoneWindow] creates the [ActionMode.TYPE_FLOATING]
 * — which bypasses [android.view.Window.Callback.onWindowStartingActionMode]
 * when AppCompat returns null for floating modes.
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
     * The PDF screen should assign this to return the ViewModel's current page value.
     */
    var getCurrentPage: (() -> Int)? = null

    private var pendingDocumentUri: Uri? = null
    private var pdfViewRef: PdfView? = null
    private var viewportListener: PdfView.OnViewportChangedListener? = null

    /**
     * Wraps the view produced by [PdfViewerFragment] in a [PdfSelectionInterceptorLayout]
     * so we can intercept [startActionModeForChild] before the action mode is created.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?,
    ): View? {
        val pdfView = super.onCreateView(inflater, container, savedInstanceState) ?: return null
        return PdfSelectionInterceptorLayout(requireContext(), pdfView) { action, text ->
            val page = getCurrentPage?.invoke() ?: 0
            onTextSelectionAction?.invoke(action, text, page)
        }
    }

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

// ── startActionModeForChild interceptor ──────────────────────────────────────

private const val MENU_ID_HIGHLIGHT = 0xBEEF_01
private const val MENU_ID_NOTE = 0xBEEF_02
private const val MENU_ID_SHARE_PDF = 0xBEEF_03

/**
 * A transparent [FrameLayout] wrapper that intercepts [startActionModeForChild].
 *
 * When [android.view.PdfView] (or any descendant) starts a floating action mode for
 * text selection, the call bubbles up through [startActionModeForChild] before
 * [android.view.PhoneWindow] creates the [ActionMode]. By wrapping the callback here —
 * before it reaches the Window — [PhoneWindow] will create the [ActionMode] with our
 * wrapper, so our custom menu items are actually shown and handled.
 */
private class PdfSelectionInterceptorLayout(
    context: Context,
    child: View,
    private val onActionTriggered: (PdfTextAction, String?) -> Unit,
) : FrameLayout(context) {

    init {
        addView(child, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun startActionModeForChild(
        originalView: View,
        callback: ActionMode.Callback,
        type: Int,
    ): ActionMode? = super.startActionModeForChild(
        originalView,
        PdfActionModeCallbackWrapper(callback, onActionTriggered, context),
        type,
    )

    override fun startActionModeForChild(
        originalView: View,
        callback: ActionMode.Callback,
    ): ActionMode? = super.startActionModeForChild(
        originalView,
        PdfActionModeCallbackWrapper(callback, onActionTriggered, context),
    )
}

/**
 * Wraps an [ActionMode.Callback] to add custom items (Highlight, Add Note, Share)
 * to the PDF text selection menu, and handle their clicks.
 */
private class PdfActionModeCallbackWrapper(
    private val original: ActionMode.Callback,
    private val onActionTriggered: (PdfTextAction, String?) -> Unit,
    private val context: Context,
) : ActionMode.Callback {

    private var savedMenu: Menu? = null

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val result = original.onCreateActionMode(mode, menu)
        menu.add(Menu.NONE, MENU_ID_HIGHLIGHT, 100, "Highlight")
        menu.add(Menu.NONE, MENU_ID_NOTE, 101, "Add Note")
        menu.add(Menu.NONE, MENU_ID_SHARE_PDF, 102, "Share")
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
                    MENU_ID_NOTE -> PdfTextAction.NOTE
                    else -> PdfTextAction.SHARE
                }

                // Trigger Copy to get selected text on the clipboard, then read it back.
                // Copy runs synchronously on the main thread so the clipboard is ready immediately.
                val text = extractSelectedText(mode)

                if (action == PdfTextAction.SHARE) {
                    val shareText = text ?: ""
                    if (shareText.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        runCatching { context.startActivity(Intent.createChooser(intent, null)) }
                    }
                } else {
                    onActionTriggered(action, text)
                }
                mode.finish()
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
     * Attempts to extract selected text by triggering the Copy item on the original
     * callback (which copies to clipboard synchronously) and immediately reading back.
     */
    private fun extractSelectedText(mode: ActionMode): String? {
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
            ?: return null
        val copyItem = savedMenu?.findItem(android.R.id.copy) ?: return null
        runCatching { original.onActionItemClicked(mode, copyItem) }
        return clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
    }
}

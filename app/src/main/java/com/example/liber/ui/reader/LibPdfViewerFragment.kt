package com.example.liber.ui.reader

import android.graphics.RectF
import android.util.SparseArray
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.PdfDocument
import androidx.pdf.view.PdfView

/**
 * Thin subclass of [PdfViewerFragment] that bridges page-change and document-load
 * events back to Compose via simple lambda callbacks.
 *
 * Named class (not anonymous) because FragmentManager requires a no-arg constructor.
 */
class LibPdfViewerFragment : androidx.pdf.viewer.fragment.PdfViewerFragment() {

    /** Invoked once the document finishes loading; receives total page count. */
    var onDocumentLoaded: ((pageCount: Int) -> Unit)? = null

    /** Invoked whenever the first visible page index (0-indexed) changes. */
    var onPageChanged: ((page: Int) -> Unit)? = null

    private var pdfViewRef: PdfView? = null
    private var viewportListener: PdfView.OnViewportChangedListener? = null

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
        pdfViewRef?.scrollToPage(pageNum)
    }
}

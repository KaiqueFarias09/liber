package org.coolreader.crengine

import android.graphics.Bitmap
import java.util.Properties

/**
 * Kotlin mirror of CoolReader's DocView Java class.
 *
 * This class MUST live in the package `org.coolreader.crengine` because the JNI
 * layer registers native methods for exactly that class name via RegisterNatives
 * in JNI_OnLoad (cr3engine.cpp).
 *
 * The field `mNativeObject` stores the C++ DocViewNative* pointer and is accessed
 * directly by the JNI (docview.cpp, getNative()).
 *
 * All public methods are synchronized on [mutex] to match the thread-safety
 * contract of the original Java DocView.
 */
class DocView(private val mutex: Any = Any()) {

    // ── Native peer ──────────────────────────────────────────────────────────
    // docview.cpp reads this field by name "mNativeObject" as a jlong.
    @JvmField
    @Suppress("unused")
    var mNativeObject: Long = 0

    // ── Callback – must be non-null before any load call ────────────────────
    // docview.cpp DocViewCallback reads field "readerCallback" on the Java object.
    @JvmField
    var readerCallback: ReaderCallback = object : ReaderCallback {}

    // ── Lifecycle ────────────────────────────────────────────────────────────

    fun create() = synchronized(mutex) { createInternal() }
    fun destroy() = synchronized(mutex) { destroyInternal() }

    // ── Document loading ─────────────────────────────────────────────────────

    fun loadDocument(path: String): Boolean = synchronized(mutex) {
        loadDocumentInternal(path)
    }

    fun loadDocumentFromBuffer(data: ByteArray, contentPath: String): Boolean =
        synchronized(mutex) { loadDocumentFromMemoryInternal(data, contentPath) }

    // ── Rendering ────────────────────────────────────────────────────────────

    /** Renders the current view into [bitmap] (must be ARGB_8888, correct size). */
    fun getPageImage(bitmap: Bitmap) = synchronized(mutex) {
        // Native expects bpp (bits-per-pixel). ARGB_8888 is always 32bpp.
        getPageImageInternal(bitmap, 32)
    }

    fun resize(width: Int, height: Int) = synchronized(mutex) {
        resizeInternal(width, height)
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    fun doCommand(cmd: Int, param: Int = 0): Boolean = synchronized(mutex) {
        doCommandInternal(cmd, param)
    }

    fun goToPosition(xpointer: String, precise: Boolean = false): Boolean =
        synchronized(mutex) { goToPositionInternal(xpointer, precise) }

    fun goLink(link: String): Int = synchronized(mutex) { goLinkInternal(link) }
    fun checkLink(x: Int, y: Int, delta: Int): String? =
        synchronized(mutex) { checkLinkInternal(x, y, delta) }

    // ── Settings ─────────────────────────────────────────────────────────────

    fun getSettings(): Properties = synchronized(mutex) { getSettingsInternal() }
    fun applySettings(props: Properties): Boolean =
        synchronized(mutex) { applySettingsInternal(props) }

    // ── Position & progress ──────────────────────────────────────────────────

    fun getPositionProps(xpointer: String = "", precise: Boolean = false): PositionProperties? =
        synchronized(mutex) { getPositionPropsInternal(xpointer, precise) }

    fun getCurrentPageBookmark(): Bookmark? =
        synchronized(mutex) { getCurrentPageBookmarkInternal() }

    // ── Table of contents ────────────────────────────────────────────────────

    fun getTOC(): TOCItem = synchronized(mutex) { getTOCInternal() }

    // ── Selection ─────────────────────────────────────────────────────────────

    /**
     * Asks the engine to compute the text selection spanning from (startX,startY)
     * to (endX,endY) in screen-pixel coordinates. On return, [sel].text, startPos,
     * and endPos are populated by the native layer.
     */
    fun updateSelection(sel: Selection) = synchronized(mutex) { updateSelectionInternal(sel) }

    // ── Highlights & bookmarks ───────────────────────────────────────────────

    fun hilightBookmarks(bookmarks: Array<Bookmark>) = synchronized(mutex) {
        hilightBookmarksInternal(bookmarks)
    }

    fun checkBookmark(x: Int, y: Int, bookmark: Bookmark): Boolean = synchronized(mutex) {
        checkBookmarkInternal(x, y, bookmark)
    }

    // ── Search ───────────────────────────────────────────────────────────────

    fun findText(pattern: String, origin: Int, reverse: Int, caseInsensitive: Int): Boolean =
        synchronized(mutex) { findTextInternal(pattern, origin, reverse, caseInsensitive) }

    fun clearSelection() = synchronized(mutex) { clearSelectionInternal() }

    // ── Misc ─────────────────────────────────────────────────────────────────

    fun swapToCache(): Int = synchronized(mutex) { swapToCacheInternal() }
    fun isRendered(): Boolean = synchronized(mutex) { isRenderedInternal() }
    fun isTimeChanged(): Boolean = synchronized(mutex) { isTimeChangedInternal() }

    // ── Native declarations (must match sDocViewMethods in cr3engine.cpp) ───

    private external fun createInternal()
    private external fun destroyInternal()
    private external fun getPageImageInternal(bitmap: Bitmap, bpp: Int)
    private external fun loadDocumentInternal(path: String): Boolean
    private external fun loadDocumentFromMemoryInternal(data: ByteArray, contentPath: String): Boolean
    private external fun getSettingsInternal(): Properties
    private external fun getDocPropsInternal(): Properties
    private external fun applySettingsInternal(props: Properties): Boolean
    private external fun setStylesheetInternal(css: String)
    private external fun resizeInternal(width: Int, height: Int)
    private external fun doCommandInternal(cmd: Int, param: Int): Boolean
    private external fun getCurrentPageBookmarkInternal(): Bookmark?
    private external fun goToPositionInternal(path: String, precise: Boolean): Boolean
    private external fun getPositionPropsInternal(path: String, precise: Boolean): PositionProperties?
    private external fun updateBookInfoInternal(info: BookInfo, updatePath: Boolean)
    private external fun getTOCInternal(): TOCItem
    private external fun clearSelectionInternal()
    private external fun findTextInternal(pattern: String, origin: Int, reverse: Int, caseInsensitive: Int): Boolean
    private external fun setBatteryStateInternal(state: Int, chargingConn: Int, chargeLevel: Int)
    private external fun getCoverPageDataInternal(): ByteArray?
    private external fun setPageBackgroundTextureInternal(imageBytes: ByteArray, tileFlags: Int)
    private external fun updateSelectionInternal(sel: Selection)
    private external fun checkLinkInternal(x: Int, y: Int, delta: Int): String?
    private external fun goLinkInternal(link: String): Int
    private external fun moveSelectionInternal(sel: Selection, cmd: Int, param: Int): Boolean
    private external fun swapToCacheInternal(): Int
    private external fun checkImageInternal(x: Int, y: Int, imageInfo: ImageInfo): Boolean
    private external fun drawImageInternal(bitmap: Bitmap, bpp: Int, imageInfo: ImageInfo): Boolean
    private external fun closeImageInternal(): Boolean
    private external fun hilightBookmarksInternal(bookmarks: Array<Bookmark>)
    private external fun checkBookmarkInternal(x: Int, y: Int, bookmark: Bookmark): Boolean
    private external fun isRenderedInternal(): Boolean
    private external fun isTimeChangedInternal(): Boolean
}

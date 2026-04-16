package org.coolreader.crengine

/**
 * Callback interface required by the CREngine JNI layer (DocViewCallback in C++).
 * All methods have no-op defaults so callers only override what they need.
 */
interface ReaderCallback {
    fun OnLoadFileStart(filename: String) {}
    fun OnLoadFileFormatDetected(format: DocumentFormat): String? = null
    fun OnLoadFileEnd() {}
    fun OnLoadFileFirstPagesReady() {}
    fun OnLoadFileProgress(percent: Int): Boolean = false
    fun OnFormatStart() {}
    fun OnFormatEnd() {}
    fun OnFormatProgress(percent: Int): Boolean = false
    fun OnExportProgress(percent: Int): Boolean = false
    fun OnRequestReload(): Boolean = false
    fun OnLoadFileError(message: String) {}
    fun OnExternalLink(url: String, refId: String) {}
    fun OnImageCacheClear() {}
}

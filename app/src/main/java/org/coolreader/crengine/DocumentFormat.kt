package org.coolreader.crengine

/**
 * Mirror of CoolReader's DocumentFormat enum.
 * The `byId` static method must exist so the JNI DocViewCallback can call it
 * when reporting OnLoadFileFormatDetected.
 */
enum class DocumentFormat {
    NONE, FB2, TXT, RTF, EPUB, HTML, TXT_BOOKMARK, CHM, DOC, TCR, PDB, PRC, MOBI, LRF,
    XLS, ZIP, FB3, DOCX, ODT, UNKNOWN;

    companion object {
        @JvmStatic
        fun byId(id: Int): DocumentFormat =
            if (id >= 0 && id < values().size) values()[id] else UNKNOWN
    }
}

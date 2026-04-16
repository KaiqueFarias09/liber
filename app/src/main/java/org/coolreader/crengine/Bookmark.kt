package org.coolreader.crengine

/**
 * Mirror of CoolReader's Bookmark Java class.
 *
 * Field names and types must match EXACTLY what the JNI code accesses in
 * docview.cpp via CRStringField / CRIntField / CRLongField:
 *   startPos, endPos, titleText, posText, commentText  → String
 *   type, percent, shortcut                            → Int
 *   timeStamp                                          → Long
 */
class Bookmark {
    @JvmField var startPos: String? = null
    @JvmField var endPos: String? = null
    @JvmField var titleText: String? = null
    @JvmField var posText: String? = null
    @JvmField var commentText: String? = null
    @JvmField var type: Int = 0
    @JvmField var percent: Int = 0
    @JvmField var shortcut: Int = 0
    @JvmField var timeStamp: Long = 0L
    @JvmField var timeElapsed: Int = 0
    @JvmField var id: Long? = null

    companion object {
        // Bookmark types (from CoolReader source)
        const val TYPE_POSITION = 0
        const val TYPE_COMMENT = 1
        const val TYPE_CORRECTION = 2
    }
}

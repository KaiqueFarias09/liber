package org.coolreader.crengine

/**
 * Mirror of CoolReader's PositionProperties Java class.
 * Field names and types must match exactly what the JNI sets via CRIntField/CRStringField.
 */
class PositionProperties {
    @JvmField var x: Int = 0
    @JvmField var y: Int = 0
    @JvmField var fullHeight: Int = 0
    @JvmField var pageHeight: Int = 0
    @JvmField var pageWidth: Int = 0
    @JvmField var pageNumber: Int = 0
    @JvmField var pageCount: Int = 0
    @JvmField var pageMode: Int = 0   // 0 = scroll, 1 = single page, 2 = two-page
    @JvmField var charCount: Int = 0
    @JvmField var imageCount: Int = 0
    @JvmField var pageText: String? = null

    /** 0–10000 progress value (matches CoolReader's getPercent()). */
    fun getPercent(): Int {
        if (fullHeight - pageHeight <= 0) return 0
        return (10000L * y / (fullHeight - pageHeight)).toInt().coerceIn(0, 10000)
    }

    /** 0.0–1.0 progress for Compose UI. */
    fun progressFraction(): Float = getPercent() / 10000f
}

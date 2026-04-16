package org.coolreader.crengine

import android.graphics.Bitmap

/**
 * Kotlin mirror of CoolReader's Engine Java class.
 * Lives in `org.coolreader.crengine` to match the JNI RegisterNatives registration.
 */
object Engine {

    /** Hyphenation dictionary descriptor passed to [initDictionaries]. */
    class HyphDict(
        @JvmField val type: Int,
        @JvmField val code: String,
        @JvmField val name: String,
        @JvmField val language: String
    )

    fun init(fontArray: Array<String>, sdkInt: Int): Boolean =
        initInternal(fontArray, sdkInt)

    fun uninit() = uninitInternal()

    fun setCacheDirectory(dir: String, sizeMb: Int): Boolean =
        setCacheDirectoryInternal(dir, sizeMb * 1024 * 1024)

    fun getFontFaceList(): Array<String> = getFontFaceListInternal()

    // ── Native declarations ──────────────────────────────────────────────────

    @JvmStatic private external fun initInternal(fontArray: Array<String>, sdkInt: Int): Boolean
    @JvmStatic private external fun uninitInternal()
    @JvmStatic external fun initDictionaries(dicts: Array<HyphDict>): Boolean
    @JvmStatic private external fun getFontFaceListInternal(): Array<String>
    @JvmStatic external fun getFontFileNameListInternal(): Array<String>
    @JvmStatic external fun getAvailableFontWeightInternal(face: String): IntArray
    @JvmStatic external fun getAvailableSynthFontWeightInternal(): IntArray
    @JvmStatic private external fun setCacheDirectoryInternal(dir: String, size: Int): Boolean
    @JvmStatic external fun scanBookPropertiesInternal(fileInfo: FileInfo): Boolean
    @JvmStatic external fun updateFileCRC32Internal(fileInfo: FileInfo): Boolean
    @JvmStatic external fun isArchiveInternal(path: String): Boolean
    @JvmStatic external fun getArchiveItemsInternal(path: String): Array<String>
    @JvmStatic external fun isLink(path: String): String?
    @JvmStatic private external fun suspendLongOperationInternal()
    @JvmStatic external fun setKeyBacklightInternal(level: Int): Boolean
    @JvmStatic external fun scanBookCoverInternal(path: String): ByteArray?
    @JvmStatic external fun drawBookCoverInternal(
        bitmap: Bitmap,
        coverData: ByteArray,
        forceEmpty: Boolean,
        title: String,
        authors: String,
        seriesName: String,
        seriesNumber: String,
        width: Int,
        height: Int
    )
    @JvmStatic external fun checkFontLanguageCompatibilityInternal(face: String, language: String): Int
    @JvmStatic external fun getHumanReadableLocaleNameInternal(locale: String): String
    @JvmStatic external fun listFilesInternal(dir: java.io.File): Array<java.io.File>?
    @JvmStatic external fun getDomVersionCurrent(): Int
}

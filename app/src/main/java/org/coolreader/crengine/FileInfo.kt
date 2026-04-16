package org.coolreader.crengine

class FileInfo {
    @JvmField var pathname: String = ""
    @JvmField var arcname: String? = null
    @JvmField var path: String = ""
    @JvmField var filename: String = ""
    @JvmField var title: String = ""
    @JvmField var authors: String = ""
    @JvmField var series: String = ""
    @JvmField var seriesNumber: Int = 0
    @JvmField var language: String = ""
    @JvmField var crc32: Long = 0L
    @JvmField var genres: String = ""
    @JvmField var description: String = ""
    @JvmField var format: DocumentFormat? = null
    @JvmField var arcsize: Long = 0L
    @JvmField var isArchive: Boolean = false
    @JvmField var size: Long = 0L
}

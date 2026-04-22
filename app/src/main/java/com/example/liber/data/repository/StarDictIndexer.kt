package com.example.liber.data.repository

import android.util.Log
import com.example.liber.data.local.DictionaryDao
import com.example.liber.data.model.DictionaryEntry
import com.example.liber.data.model.DictionarySense
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StarDictIndexer @Inject constructor(
    private val dictionaryDao: DictionaryDao
) {
    private companion object {
        const val TAG = "StarDictIndexer"
        const val BATCH_SIZE = 500
    }

    suspend fun index(dictionaryId: String, archiveFile: File, languageTag: String) {
        Log.d(TAG, "Starting indexing for $dictionaryId from ${archiveFile.absolutePath}")
        
        var ifoData: String? = null
        var idxData: ByteArray? = null
        var dictData: ByteArray? = null

        try {
            FileInputStream(archiveFile).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    XZCompressorInputStream(bis).use { xzis ->
                        TarArchiveInputStream(xzis).use { tais ->
                            var entry = tais.nextEntry
                            while (entry != null) {
                                val name = entry.name.lowercase(Locale.ROOT)
                                when {
                                    name.endsWith(".ifo") -> ifoData = readEntry(tais)
                                    name.endsWith(".idx") -> idxData = readEntryBytes(tais)
                                    name.endsWith(".dict") -> dictData = readEntryBytes(tais)
                                }
                                entry = tais.nextEntry
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting dictionary archive", e)
            return
        }

        if (idxData == null || dictData == null) {
            Log.e(TAG, "Missing required files in archive (idx: ${idxData != null}, dict: ${dictData != null})")
            return
        }

        val offsetBits = if (ifoData?.contains("idxoffsetbits=64") == true) 64 else 32
        Log.d(TAG, "Extraction complete. Parsing index (offsetBits=$offsetBits)...")

        parseAndStore(dictionaryId, languageTag, idxData!!, dictData!!, offsetBits)
        Log.d(TAG, "Indexing finished for $dictionaryId")
    }

    private fun readEntry(tais: TarArchiveInputStream): String {
        return String(readEntryBytes(tais))
    }

    private fun readEntryBytes(tais: TarArchiveInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var len = tais.read(buffer)
        while (len != -1) {
            out.write(buffer, 0, len)
            len = tais.read(buffer)
        }
        return out.toByteArray()
    }

    private suspend fun parseAndStore(
        dictionaryId: String,
        languageTag: String,
        idxData: ByteArray,
        dictData: ByteArray,
        offsetBits: Int
    ) {
        val entries = mutableListOf<DictionaryEntry>()
        val definitions = mutableListOf<String>()
        
        var pos = 0
        val offsetSize = offsetBits / 8
        
        while (pos < idxData.size) {
            // Read headword (null-terminated)
            val start = pos
            while (pos < idxData.size && idxData[pos] != 0.toByte()) {
                pos++
            }
            if (pos >= idxData.size) break
            
            val headword = String(idxData, start, pos - start, Charsets.UTF_8)
            pos++ // skip null

            // Read offset and size
            if (pos + offsetSize + 4 > idxData.size) break
            
            val offset = if (offsetBits == 64) {
                ByteBuffer.wrap(idxData, pos, 8).order(ByteOrder.BIG_ENDIAN).long
            } else {
                ByteBuffer.wrap(idxData, pos, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()
            }
            pos += offsetSize
            
            val size = ByteBuffer.wrap(idxData, pos, 4).order(ByteOrder.BIG_ENDIAN).int
            pos += 4

            val entry = DictionaryEntry(
                dictionaryId = dictionaryId,
                headword = headword,
                normalizedHeadword = headword.lowercase(Locale.ROOT).trim(),
                languageTag = languageTag
            )
            entries.add(entry)

            // Read definition from dictData
            if (offset >= 0 && offset + size <= dictData.size) {
                val definition = String(dictData, offset.toInt(), size, Charsets.UTF_8).trim()
                definitions.add(definition)
            } else {
                definitions.add("")
            }

            if (entries.size >= BATCH_SIZE) {
                flush(entries, definitions)
                entries.clear()
                definitions.clear()
            }
        }

        if (entries.isNotEmpty()) {
            flush(entries, definitions)
        }
    }

    private suspend fun flush(
        entries: List<DictionaryEntry>,
        definitions: List<String>
    ) {
        val ids = dictionaryDao.upsertEntries(entries)
        val senses = mutableListOf<DictionarySense>()
        ids.forEachIndexed { index, entryId ->
            val definition = definitions[index]
            if (definition.isNotBlank()) {
                senses.add(
                    DictionarySense(
                        entryId = entryId,
                        definition = definition,
                        partOfSpeech = null
                    )
                )
            }
        }
        if (senses.isNotEmpty()) {
            dictionaryDao.upsertSenses(senses)
        }
    }
}

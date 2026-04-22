package com.example.liber.data.repository

import android.app.Application
import android.net.Uri
import android.util.Log
import com.example.liber.api.FreeDictApi
import com.example.liber.data.local.DictionaryDao
import com.example.liber.data.local.DictionaryEntryWithSenses
import com.example.liber.data.model.Dictionary
import com.example.liber.data.model.DictionaryLookupHistory
import com.example.liber.data.model.FreeDictCatalogItem
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale
import java.util.UUID

class DictionaryRepository(
    private val dictionaryDao: DictionaryDao,
    private val freeDictApi: FreeDictApi,
    application: Application,
) {

    private val appContext = application.applicationContext
    private val httpClient = OkHttpClient()

    fun getAllDictionaries(): Flow<List<Dictionary>> = dictionaryDao.getAllDictionaries()

    fun getLookupHistory(limit: Int = 100): Flow<List<DictionaryLookupHistory>> =
        dictionaryDao.getLookupHistory(limit)

    suspend fun createManualDictionary(
        displayName: String,
        sourceLanguageTag: String,
        targetLanguageTag: String?,
        dictionaryType: String,
        uri: Uri?,
    ) {
        val now = System.currentTimeMillis()
        var localFilePath: String? = null
        var fileSize: Long = 0

        if (uri != null) {
            val dictionariesDir = File(appContext.filesDir, "dictionaries")
            if (!dictionariesDir.exists()) {
                dictionariesDir.mkdirs()
            }

            val documentFile =
                androidx.documentfile.provider.DocumentFile.fromSingleUri(appContext, uri)
            val fileName = documentFile?.name ?: "manual_dict_${System.currentTimeMillis()}"
            val targetFile = File(dictionariesDir, fileName)

            appContext.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            localFilePath = targetFile.absolutePath
            fileSize = targetFile.length()
        }

        dictionaryDao.upsertDictionary(
            Dictionary(
                id = UUID.randomUUID().toString(),
                displayName = displayName,
                sourceLanguageTag = sourceLanguageTag,
                targetLanguageTag = targetLanguageTag,
                dictionaryType = dictionaryType,
                packageFormat = "manual",
                version = "local-1",
                localFilePath = localFilePath,
                remoteUrl = null,
                installSizeBytes = fileSize,
                installedAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun fetchFreeDictCatalog(): List<FreeDictCatalogItem> {
        return freeDictApi.getDatabase()
            .mapNotNull { dto ->
                val name = dto.name ?: return@mapNotNull null
                val stardict = dto.releases
                    .filter { it.platform.equals("stardict", ignoreCase = true) }
                    .maxByOrNull { it.date.orEmpty() }
                    ?: return@mapNotNull null

                val parts = name.split("-")
                if (parts.size < 2) return@mapNotNull null

                FreeDictCatalogItem(
                    code = name,
                    sourceLanguageTag = parts.first(),
                    targetLanguageTag = parts.last(),
                    version = stardict.version,
                    headwords = dto.headwords?.toIntOrNull(),
                    stardictUrl = stardict.url,
                    stardictSizeBytes = stardict.size?.toLongOrNull(),
                )
            }
            .sortedWith(compareBy({ it.sourceLanguageTag }, { it.targetLanguageTag }))
    }

    suspend fun downloadAndInstallFreeDict(item: FreeDictCatalogItem): Dictionary {
        val dictionariesDir = File(appContext.filesDir, "dictionaries")
        if (!dictionariesDir.exists()) {
            dictionariesDir.mkdirs()
        }

        val fileName = "${item.code}-${item.version}.stardict.tar.xz"
        val targetFile = File(dictionariesDir, fileName)

        val request = Request.Builder()
            .url(item.stardictUrl)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to download dictionary (${response.code})")
            }
            val body = response.body ?: throw IllegalStateException("Empty dictionary response")
            body.byteStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val now = now()
        val id = "freedict-${item.code}"
        val existing = dictionaryDao.getDictionaryById(id)
        val dictionary = Dictionary(
            id = id,
            displayName = item.code,
            localAlias = existing?.localAlias,
            sourceLanguageTag = item.sourceLanguageTag,
            targetLanguageTag = item.targetLanguageTag,
            dictionaryType = "bilingual",
            packageFormat = "stardict.tar.xz",
            version = item.version,
            localFilePath = targetFile.absolutePath,
            remoteUrl = item.stardictUrl,
            installSizeBytes = targetFile.length(),
            isEnabled = existing?.isEnabled ?: false,
            priority = existing?.priority ?: 100,
            installedAt = existing?.installedAt ?: now,
            updatedAt = now,
        )
        dictionaryDao.upsertDictionary(dictionary)
        return dictionary
    }

    suspend fun renameDictionary(id: String, localAlias: String?) {
        dictionaryDao.setDictionaryAlias(id, localAlias?.trim()?.ifEmpty { null }, now())
    }

    suspend fun setDictionaryEnabled(id: String, enabled: Boolean) {
        dictionaryDao.setDictionaryEnabled(id, enabled, now())
    }

    suspend fun setDictionaryPriority(id: String, priority: Int) {
        dictionaryDao.setDictionaryPriority(id, priority, now())
    }

    suspend fun deleteDictionary(id: String) {
        dictionaryDao.deleteDictionary(id)
    }

    suspend fun searchEntries(
        query: String,
        languageTag: String,
        limit: Int = 30,
    ): List<DictionaryEntryWithSenses> {
        val trimmed = query.trim()
        val normalizedTag = normalizeLanguageTag(languageTag)
        Log.d("DictionaryRepository", "Searching for '$trimmed' with languageTag '$languageTag' (normalized: '$normalizedTag')")
        if (trimmed.isBlank()) return emptyList()

        val normalizedPrefix = normalize(trimmed) + "%"
        val rawPrefix = trimmed + "%"
        val entries = dictionaryDao.searchEntries(normalizedTag, normalizedPrefix, rawPrefix, limit)
        Log.d("DictionaryRepository", "Found ${entries.size} entries in database")
        if (entries.isEmpty()) return emptyList()

        return dictionaryDao.getEntriesWithSenses(entries.map { it.id })
    }

    private fun normalizeLanguageTag(tag: String): String {
        val iso2 = tag.split("-").first().lowercase(Locale.ROOT)
        // Basic mapping for common languages to ISO 639-2/T (used by FreeDict)
        return when (iso2) {
            "en" -> "eng"
            "pt" -> "por"
            "fr" -> "fra"
            "de" -> "deu"
            "es" -> "spa"
            "it" -> "ita"
            "ru" -> "rus"
            "nl" -> "nld"
            "sv" -> "swe"
            "fi" -> "fin"
            "da" -> "dan"
            "no" -> "nor"
            "ja" -> "jpn"
            "zh" -> "zho"
            "ko" -> "kor"
            "ar" -> "ara"
            "hi" -> "hin"
            else -> {
                // Try using Java Locale to get ISO3 if not in map
                try {
                    Locale.Builder().setLanguage(iso2).build().isO3Language.lowercase(Locale.ROOT)
                } catch (_: Exception) {
                    iso2
                }
            }
        }
    }

    suspend fun addLookupHistory(
        query: String,
        entryId: Long?,
        dictionaryId: String?,
        sourceBookId: String?,
    ) {
        dictionaryDao.insertLookupHistory(
            DictionaryLookupHistory(
                query = query,
                entryId = entryId,
                dictionaryId = dictionaryId,
                sourceBookId = sourceBookId,
            )
        )
    }

    suspend fun deleteLookupHistory(id: Long) {
        dictionaryDao.deleteLookupHistory(id)
    }

    suspend fun clearLookupHistory() {
        dictionaryDao.clearLookupHistory()
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun normalize(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }
}

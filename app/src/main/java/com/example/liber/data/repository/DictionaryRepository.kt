package com.example.liber.data.repository

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.liber.R
import com.example.liber.api.FreeDictApi
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseRepository
import com.example.liber.data.local.DictionaryDao
import com.example.liber.data.local.DictionaryEntryWithSenses
import com.example.liber.data.local.WordLemmaDao
import com.example.liber.data.model.Dictionary
import com.example.liber.data.model.DictionaryLookupHistory
import com.example.liber.data.model.FreeDictCatalogItem
import com.example.liber.data.model.WordLemma
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID

class DictionaryRepository(
    private val dictionaryDao: DictionaryDao,
    private val wordLemmaDao: WordLemmaDao,
    private val freeDictApi: FreeDictApi,
    private val starDictIndexer: StarDictIndexer,
    application: Application,
    appLogger: AppLogger,
) : BaseRepository("DictionaryRepository", appLogger) {

    private val appContext = application.applicationContext
    private val httpClient = OkHttpClient()

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "dictionary_tasks"
        private const val LEMMA_NOTIFICATION_ID_BASE = 2000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dictionary Tasks",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for dictionary and lemmatization downloads"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val _lemmatizationStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val lemmatizationStatus: StateFlow<Map<String, String>> = _lemmatizationStatus.asStateFlow()

    val languagesWithLemmas: Flow<Set<String>> = wordLemmaDao.getLanguagesWithLemmas()
        .map { it.toSet() }

    fun getAllDictionaries(): Flow<List<Dictionary>> = observeOperation(
        "getAllDictionaries",
        upstream = dictionaryDao.getAllDictionaries(),
    )

    fun getLookupHistory(limit: Int = 100): Flow<List<DictionaryLookupHistory>> =
        observeOperation(
            operationName = "getLookupHistory",
            parameters = mapOf("limit" to limit),
            upstream = dictionaryDao.getLookupHistory(limit),
        )

    suspend fun createManualDictionary(
        displayName: String,
        sourceLanguageTag: String,
        targetLanguageTag: String?,
        dictionaryType: String,
        uri: Uri?,
    ) = executeOperation(
        operationName = "createManualDictionary",
        parameters = mapOf(
            "displayName" to displayName,
            "sourceLanguageTag" to sourceLanguageTag,
            "targetLanguageTag" to targetLanguageTag,
            "dictionaryType" to dictionaryType,
            "hasUri" to (uri != null),
        ),
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

    suspend fun fetchFreeDictCatalog(): List<FreeDictCatalogItem> = executeOperation(
        operationName = "fetchFreeDictCatalog",
    ) {
        freeDictApi.getDatabase()
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

    suspend fun downloadAndInstallFreeDict(item: FreeDictCatalogItem): Dictionary =
        executeOperation(
            operationName = "downloadAndInstallFreeDict",
            parameters = mapOf("code" to item.code, "version" to item.version),
        ) {
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
                val body = response.body
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

            // Trigger indexing
            try {
                val normalizedTag = normalizeLanguageTag(item.sourceLanguageTag)
                starDictIndexer.index(id, targetFile, normalizedTag)

                // Check and download lemmatization data if needed
                ensureLemmatizationData(normalizedTag)
            } catch (e: Exception) {
                logger.recordError(
                    throwable = e,
                    operationName = "downloadAndInstallFreeDict",
                    message = "Failed to index dictionary: $id",
                )
            }

            return dictionary
        }

    suspend fun renameDictionary(id: String, localAlias: String?) = executeOperation(
        operationName = "renameDictionary",
        parameters = mapOf("id" to id, "hasLocalAlias" to !localAlias.isNullOrBlank()),
    ) {
        dictionaryDao.setDictionaryAlias(id, localAlias?.trim()?.ifEmpty { null }, now())
    }

    suspend fun setDictionaryEnabled(id: String, enabled: Boolean) = executeOperation(
        operationName = "setDictionaryEnabled",
        parameters = mapOf("id" to id, "enabled" to enabled),
    ) {
        dictionaryDao.setDictionaryEnabled(id, enabled, now())
    }

    suspend fun setDictionaryPriority(id: String, priority: Int) = executeOperation(
        operationName = "setDictionaryPriority",
        parameters = mapOf("id" to id, "priority" to priority),
    ) {
        dictionaryDao.setDictionaryPriority(id, priority, now())
    }

    suspend fun deleteDictionary(id: String) = executeOperation(
        operationName = "deleteDictionary",
        parameters = mapOf("id" to id),
    ) {
        dictionaryDao.deleteDictionary(id)
    }

    suspend fun searchEntries(
        query: String,
        languageTag: String,
        limit: Int = 30,
    ): List<DictionaryEntryWithSenses> = executeOperation(
        operationName = "searchEntries",
        parameters = mapOf("query" to query.trim(), "languageTag" to languageTag, "limit" to limit),
    ) {
        val trimmed = query.trim()
        val normalizedTag = normalizeLanguageTag(languageTag)
        logger.log(
            "Searching for \"$trimmed\" with languageTag=\"$languageTag\" normalized=\"$normalizedTag\""
        )
        if (trimmed.isBlank()) return emptyList()

        // 1. Get potential lemmas for the query
        val lemmas = wordLemmaDao.findLemmas(trimmed.lowercase(Locale.ROOT), normalizedTag)
        val searchTerms = (listOf(trimmed) + lemmas).distinct()

        if (lemmas.isNotEmpty()) {
            logger.log("Found lemmas for \"$trimmed\": $lemmas")
        }

        val entries = mutableListOf<com.example.liber.data.model.DictionaryEntry>()

        // 2. Search for each term (original + lemmas)
        for (term in searchTerms) {
            val normalizedPrefix = normalize(term) + "%"
            val rawPrefix = term + "%"
            val results =
                dictionaryDao.searchEntries(normalizedTag, normalizedPrefix, rawPrefix, limit)
            entries.addAll(results)
            if (entries.size >= limit) break
        }

        val distinctEntries = entries.distinctBy { it.id }.take(limit)
        logger.log("Found ${distinctEntries.size} entries in database")
        if (distinctEntries.isEmpty()) return emptyList()

        return dictionaryDao.getEntriesWithSenses(distinctEntries.map { it.id })
    }

    suspend fun getEntriesByDictionary(
        dictionaryId: String,
        limit: Int = 50,
        offset: Int = 0,
    ): List<DictionaryEntryWithSenses> = executeOperation(
        operationName = "getEntriesByDictionary",
        parameters = mapOf("dictionaryId" to dictionaryId, "limit" to limit, "offset" to offset),
    ) {
        dictionaryDao.getEntriesByDictionary(dictionaryId, limit, offset)
    }

    suspend fun searchEntriesInDictionary(
        dictionaryId: String,
        query: String,
        limit: Int = 50,
        offset: Int = 0,
    ): List<DictionaryEntryWithSenses> = executeOperation(
        operationName = "searchEntriesInDictionary",
        parameters = mapOf("dictionaryId" to dictionaryId, "query" to query, "limit" to limit, "offset" to offset),
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@executeOperation dictionaryDao.getEntriesByDictionary(dictionaryId, limit, offset)
        }

        // 1. Find the dictionary to get the source language
        val dict = dictionaryDao.getDictionaryById(dictionaryId)
        val searchTerms = if (dict != null) {
            val normalizedTag = normalizeLanguageTag(dict.sourceLanguageTag)
            val lemmas = wordLemmaDao.findLemmas(trimmed.lowercase(Locale.ROOT), normalizedTag)
            (listOf(trimmed) + lemmas).distinct()
        } else {
            listOf(trimmed)
        }

        // 2. Perform search with expanded terms
        dictionaryDao.searchEntriesInDictionary(
            dictionaryId = dictionaryId,
            query = "%$trimmed%",
            pureQuery = trimmed,
            prefix = "$trimmed%",
            extraTerms = searchTerms,
            limit = limit,
            offset = offset
        )
    }

    private suspend fun ensureLemmatizationData(languageTag: String) {
        val count = wordLemmaDao.getCountForLanguage(languageTag)
        if (count > 0) {
            logger.log("Lemmatization data for $languageTag already exists ($count entries).")
            return
        }

        val notificationId = LEMMA_NOTIFICATION_ID_BASE + languageTag.hashCode()
        val languageName = getLanguageName(languageTag)

        fun updateProgress(status: String, progress: Int = -1) {
            updateLemmatizationStatus(languageTag, status)
            val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle("Downloading Lemmas: $languageName")
                .setContentText(status)
                .setOnlyAlertOnce(true)
                .setOngoing(true)

            if (progress >= 0) {
                builder.setProgress(100, progress, false)
            } else {
                builder.setProgress(0, 0, true)
            }
            notificationManager.notify(notificationId, builder.build())
        }

        updateProgress("Downloading...")
        logger.log("Downloading lemmatization data for $languageTag...")
        val url = "https://raw.githubusercontent.com/unimorph/$languageTag/master/$languageTag"

        val request = Request.Builder().url(url).build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.log("No UniMorph data found for $languageTag at $url")
                    updateLemmatizationStatus(languageTag, null)
                    notificationManager.cancel(notificationId)
                    return
                }
                val body = response.body

                val totalBytes = body.contentLength()
                var bytesRead = 0L
                val lemmas = mutableListOf<WordLemma>()

                InputStreamReader(body.byteStream()).buffered().use { reader ->
                    var line = reader.readLine()
                    var lastUpdate = 0L
                    while (line != null) {
                        bytesRead += line.length + 1 // roughly
                        val parts = line.split("\t")
                        if (parts.size >= 2) {
                            lemmas.add(
                                WordLemma(
                                    languageTag = languageTag,
                                    lemma = parts[0].trim(),
                                    inflection = parts[1].trim()
                                )
                            )
                        }

                        if (lemmas.size >= 1000) {
                            wordLemmaDao.insertLemmas(lemmas.toList())
                            lemmas.clear()

                            // Update notification progress every 1s or so to avoid spamming
                            val now = System.currentTimeMillis()
                            if (now - lastUpdate > 1000) {
                                val progress =
                                    if (totalBytes > 0) ((bytesRead * 100) / totalBytes).toInt() else -1
                                updateProgress("Importing... $progress%", progress)
                                lastUpdate = now
                            }
                        }
                        line = reader.readLine()
                    }
                }

                if (lemmas.isNotEmpty()) {
                    wordLemmaDao.insertLemmas(lemmas)
                }
                logger.log("Lemmatization data for $languageTag imported successfully.")
                updateLemmatizationStatus(languageTag, null)

                // Show final success notification briefly then clear
                notificationManager.notify(
                    notificationId,
                    NotificationCompat.Builder(appContext, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher_foreground)
                        .setContentTitle("Lemmas Ready: $languageName")
                        .setContentText("Import complete.")
                        .setAutoCancel(true)
                        .build()
                )
            }
        } catch (e: Exception) {
            logger.log("Failed to download lemmatization data for $languageTag: ${e.message}")
            updateLemmatizationStatus(languageTag, null)
            notificationManager.cancel(notificationId)
        }
    }

    private fun getLanguageName(tag: String): String {
        return try {
            Locale.forLanguageTag(tag).getDisplayLanguage(Locale.ENGLISH)
                .ifEmpty { tag.uppercase(Locale.ROOT) }
        } catch (_: Exception) {
            tag.uppercase(Locale.ROOT)
        }
    }

    private fun updateLemmatizationStatus(languageTag: String, status: String?) {
        val current = _lemmatizationStatus.value.toMutableMap()
        if (status == null) {
            current.remove(languageTag)
        } else {
            current[languageTag] = status
        }
        _lemmatizationStatus.value = current
    }

    fun normalizeLanguageTag(tag: String): String {
        val primary = tag.split("-", "_").first().lowercase(Locale.ROOT)

        // If it's already a 3-letter code, return it (likely ISO 639-2)
        if (primary.length == 3) return primary

        // Basic mapping for common 2-letter codes to ISO 639-2/T (used by FreeDict)
        return when (primary) {
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
                try {
                    // Try to get ISO 639-2/T code from Java Locale
                    Locale.forLanguageTag(primary).isO3Language.lowercase(Locale.ROOT)
                } catch (_: Exception) {
                    primary
                }
            }
        }
    }

    suspend fun addLookupHistory(
        query: String,
        entryId: Long?,
        dictionaryId: String?,
        sourceBookId: String?,
    ) = executeOperation(
        operationName = "addLookupHistory",
        parameters = mapOf(
            "query" to query,
            "entryId" to entryId,
            "dictionaryId" to dictionaryId,
            "sourceBookId" to sourceBookId,
        ),
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

    suspend fun deleteLookupHistory(id: Long) = executeOperation(
        operationName = "deleteLookupHistory",
        parameters = mapOf("id" to id),
    ) {
        dictionaryDao.deleteLookupHistory(id)
    }

    suspend fun clearLookupHistory() = executeOperation("clearLookupHistory") {
        dictionaryDao.clearLookupHistory()
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun normalize(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }
}

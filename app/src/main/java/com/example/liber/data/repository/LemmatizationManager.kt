package com.example.liber.data.repository

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.liber.R
import com.example.liber.core.logging.AppLogger
import com.example.liber.data.local.WordLemmaDao
import com.example.liber.data.model.WordLemma
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.util.Locale

class LemmatizationManager(
    private val wordLemmaDao: WordLemmaDao,
    private val application: Application,
    private val appLogger: AppLogger,
) {
    private val appContext = application.applicationContext
    private val httpClient = OkHttpClient()
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _lemmatizationStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val lemmatizationStatus: StateFlow<Map<String, String>> = _lemmatizationStatus.asStateFlow()

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

    suspend fun ensureLemmatizationData(languageTag: String) {
        val count = wordLemmaDao.getCountForLanguage(languageTag)
        if (count > 0) {
            appLogger.debug(
                "Lemmatization data for $languageTag already exists ($count entries).",
                tag = "LemmatizationManager"
            )
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
        appLogger.debug(
            "Downloading lemmatization data for $languageTag...",
            tag = "LemmatizationManager"
        )
        val url = "https://raw.githubusercontent.com/unimorph/$languageTag/master/$languageTag"

        val request = Request.Builder().url(url).build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    appLogger.debug(
                        "No UniMorph data found for $languageTag at $url",
                        tag = "LemmatizationManager"
                    )
                    updateLemmatizationStatus(languageTag, null)
                    notificationManager.cancel(notificationId)
                    return
                }
                val body = response.body ?: return

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
                appLogger.debug(
                    "Lemmatization data for $languageTag imported successfully.",
                    tag = "LemmatizationManager"
                )
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
            appLogger.error(
                "Failed to download lemmatization data for $languageTag",
                tag = "LemmatizationManager",
                throwable = e
            )
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
}

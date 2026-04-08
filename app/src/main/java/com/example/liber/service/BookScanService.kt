package com.example.liber.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import com.example.liber.R
import com.example.liber.data.AppDatabase
import com.example.liber.data.BookImporter
import com.example.liber.data.BookRepository
import com.example.liber.data.ScanSourceRepository
import com.example.liber.data.ScanState
import com.example.liber.data.ScanStateHolder
import com.example.liber.data.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BookScanService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_SCAN = "com.example.liber.action.SCAN"
        const val EXTRA_TREE_URI = "extra_tree_uri"
        const val EXTRA_FOLDER_NAME = "extra_folder_name"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "scan_channel"

        fun buildIntent(context: Context, treeUri: Uri, folderName: String): Intent =
            Intent(context, BookScanService::class.java).apply {
                action = ACTION_SCAN
                putExtra(EXTRA_TREE_URI, treeUri.toString())
                putExtra(EXTRA_FOLDER_NAME, folderName)
            }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_SCAN) return START_NOT_STICKY

        val treeUriStr = intent.getStringExtra(EXTRA_TREE_URI) ?: return START_NOT_STICKY
        val folderName = intent.getStringExtra(EXTRA_FOLDER_NAME) ?: "Folder"
        val treeUri = Uri.parse(treeUriStr)

        startForeground(NOTIFICATION_ID, buildNotification(folderName, 0, 0))

        serviceScope.launch {
            runScan(treeUri, folderName)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun runScan(treeUri: Uri, folderName: String) {
        val db = AppDatabase.getDatabase(this)
        val bookRepo = BookRepository(db.bookDao())
        val sourceRepo = ScanSourceRepository(db.scanSourceDao())
        val importer = BookImporter(application)

        val folder = DocumentFile.fromTreeUri(this, treeUri)
        if (folder == null || !folder.canRead()) {
            ScanStateHolder.update(ScanState.Failed("Cannot access folder: $folderName"))
            return
        }

        ScanStateHolder.update(ScanState.Scanning(folderName, 0, -1, 0))
        updateNotification(folderName, 0, -1)

        val bookFiles = collectBookFiles(folder)
        val total = bookFiles.size
        var newlyAdded = 0
        var skipped = 0

        bookFiles.forEachIndexed { index, file ->
            ScanStateHolder.update(ScanState.Scanning(folderName, index + 1, total, newlyAdded))
            updateNotification(folderName, index + 1, total)

            val isDuplicate = isDuplicate(file.uri, bookRepo)
            if (!isDuplicate) {
                val book = importer.parseBook(file)
                if (book != null) {
                    // Secondary dedup check by contentId after parsing
                    val existsByContentId = book.contentId
                        ?.let { bookRepo.getBookByContentId(it) } != null
                    if (!existsByContentId) {
                        bookRepo.insertBook(book.toEntity())
                        newlyAdded++
                    } else {
                        skipped++
                    }
                } else {
                    skipped++
                }
            } else {
                skipped++
            }
        }

        sourceRepo.updateScanResult(treeUri.toString(), System.currentTimeMillis(), newlyAdded)
        ScanStateHolder.update(ScanState.Finished(folderName, newlyAdded, skipped))
    }

    private suspend fun isDuplicate(uri: Uri, bookRepo: BookRepository): Boolean =
        bookRepo.getBookByFileUri(uri.toString()) != null

    private fun collectBookFiles(dir: DocumentFile): List<DocumentFile> {
        val results = mutableListOf<DocumentFile>()
        val files = dir.listFiles()

        // Check if directory itself is an audiobook (contains audio files)
        val hasAudio = files.any { it.isFile && isSupportedAudioFile(it.name) }
        if (hasAudio) {
            results += dir
        }

        files.forEach { child ->
            when {
                child.isDirectory && !hasAudio -> results += collectBookFiles(child)
                child.isFile && isSupportedFile(child.name) -> results += child
            }
        }
        return results
    }

    private fun isSupportedFile(name: String?): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase() ?: return false
        return ext in setOf("epub", "pdf") || isSupportedAudioFile(name)
    }

    private fun isSupportedAudioFile(name: String?): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase() ?: return false
        // Basic list for testing, you could add flac, ogg, etc.
        return ext in setOf("mp3", "m4a", "m4b", "aac", "wav")
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Book scanning",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows progress while scanning folders for books"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(folder: String, current: Int, total: Int): Notification {
        val text = if (total <= 0) "Scanning…" else "$current of $total books processed"
        val progress = if (total > 0) current else 0
        val indeterminate = total <= 0

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Scanning $folder")
            .setContentText(text)
            .setProgress(total.coerceAtLeast(1), progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(folder: String, current: Int, total: Int) {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTIFICATION_ID, buildNotification(folder, current, total))
    }
}

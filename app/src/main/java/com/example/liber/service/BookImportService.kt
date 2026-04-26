package com.example.liber.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.liber.R
import com.example.liber.core.logging.AndroidAppLogger
import com.example.liber.data.local.AppDatabase
import com.example.liber.data.repository.BookImporter
import com.example.liber.data.repository.BookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BookImportService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val ACTION_IMPORT = "com.example.liber.action.IMPORT"
        private const val EXTRA_URIS = "extra_uris"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "import_channel"

        fun startImport(context: Context, uris: List<Uri>) {
            val intent = Intent(context, BookImportService::class.java).apply {
                action = ACTION_IMPORT
                putParcelableArrayListExtra(EXTRA_URIS, ArrayList(uris))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_IMPORT) return START_NOT_STICKY

        val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_URIS, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Uri>(EXTRA_URIS)
        } ?: return START_NOT_STICKY

        startForeground(NOTIFICATION_ID, buildNotification(0, uris.size))

        serviceScope.launch {
            runImport(uris)
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun runImport(uris: List<Uri>) {
        val db = AppDatabase.getDatabase(this)
        val appLogger = AndroidAppLogger(applicationContext)
        val bookRepo = BookRepository(db.bookDao(), appLogger)
        val importer = BookImporter(application, appLogger)

        val total = uris.size
        uris.forEachIndexed { index, uri ->
            updateNotification(index + 1, total)
            
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                appLogger.warn("Failed to persist permission for $uri", tag = "BookImportService")
            }

            val documentFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(this, uri)
            if (documentFile != null) {
                val book = importer.parseBook(documentFile, showNotification = false)
                if (book != null) {
                    val existing = book.contentId?.let { bookRepo.getBookByContentId(it) }
                        ?: bookRepo.getBookByFileUri(uri.toString())
                    if (existing == null) {
                        bookRepo.insertBook(book)
                    }
                }
            }
        }
        
        // Final notification update to show completion
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle("Import Complete")
                .setContentText("Processed $total books.")
                .setAutoCancel(true)
                .build()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Book importing",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows progress while importing books"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(current: Int, total: Int): Notification {
        val text = if (total <= 1) "Importing book…" else "Importing $current of $total books"
        val progress = if (total > 0) current else 0

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle("Importing Books")
            .setContentText(text)
            .setProgress(total, progress, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(current: Int, total: Int) {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTIFICATION_ID, buildNotification(current, total))
    }
}

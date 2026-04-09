package com.example.liber.ui.reader

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.liber.data.Book
import com.example.liber.data.BookRepository
import com.example.liber.player.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AudiobookPlayerViewModel(
    application: Application,
    private val bookRepository: BookRepository
) : AndroidViewModel(application) {

    data class TrackInfo(val name: String, val uri: Uri)

    private var currentBookId: String? = null
    private var currentBook: Book? = null

    private val _tracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val tracks: StateFlow<List<TrackInfo>> = _tracks

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _isPrepared = MutableStateFlow(false)
    val isPrepared: StateFlow<Boolean> = _isPrepared

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private var positionUpdateJob: Job? = null

    init {
        val sessionToken =
            SessionToken(application, ComponentName(application, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = controller ?: return@addListener
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) startPositionUpdates() else positionUpdateJob?.cancel()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val controller = controller ?: return
                    _currentTrackIndex.value = controller.currentMediaItemIndex
                    _durationMs.value = controller.duration.coerceAtLeast(0L)
                    saveProgress()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _isPrepared.value =
                        playbackState != Player.STATE_IDLE && playbackState != Player.STATE_BUFFERING
                    _durationMs.value = controller.duration.coerceAtLeast(0L)
                }
            })
            // Sync initial state
            _isPlaying.value = controller.isPlaying
            _currentTrackIndex.value = controller.currentMediaItemIndex
            _durationMs.value = controller.duration.coerceAtLeast(0L)
            _positionMs.value = controller.currentPosition
            if (controller.isPlaying) startPositionUpdates()
        }, MoreExecutors.directExecutor())
    }

    fun loadBook(book: Book) {
        if (currentBookId == book.id) return

        currentBookId = book.id
        currentBook = book

        // Reset state for new book
        _isPrepared.value = false
        _tracks.value = emptyList()
        _positionMs.value = 0L
        _durationMs.value = 0L

        loadTracks(book)
    }

    private fun loadTracks(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = book.fileUri
            val trackList = if (uri.toString().contains("tree")) {
                val folder = DocumentFile.fromTreeUri(getApplication(), uri) ?: return@launch
                folder.listFiles()
                    .filter { it.isFile && isAudioFile(it.name) }
                    .sortedBy { it.name }
                    .map {
                        TrackInfo(
                            it.name?.substringBeforeLast('.') ?: it.name ?: "Track",
                            it.uri
                        )
                    }
            } else {
                val file = DocumentFile.fromSingleUri(getApplication(), uri) ?: return@launch
                listOf(TrackInfo(file.name?.substringBeforeLast('.') ?: "Track", file.uri))
            }
            _tracks.value = trackList
            if (trackList.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    prepareTracks(trackList, book)
                }
            }
        }
    }

    private fun prepareTracks(trackList: List<TrackInfo>, book: Book) {
        val controller = controller ?: return

        val mediaItems = trackList.map { track ->
            MediaItem.Builder()
                .setMediaId(track.uri.toString())
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.name)
                        .setArtist(book.author)
                        .setAlbumTitle(book.title)
                        .setArtworkUri(book.coverUri)
                        .build()
                )
                .build()
        }

        controller.setMediaItems(mediaItems)

        // Restore position
        book.lastLocator?.let { locatorStr ->
            try {
                val json = JSONObject(locatorStr)
                val trackIdx = json.getInt("trackIndex")
                val posMs = json.getLong("positionMs")
                if (trackIdx in trackList.indices) {
                    controller.seekTo(trackIdx, posMs)
                    _currentTrackIndex.value = trackIdx
                    _positionMs.value = posMs
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        controller.prepare()
        _isPrepared.value = true
    }

    fun togglePlayPause() {
        val controller = controller ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun skipForward(seconds: Int = 30) {
        val newPos = (_positionMs.value + seconds * 1000L).coerceAtMost(_durationMs.value)
        seekTo(newPos)
    }

    fun skipBackward(seconds: Int = 30) {
        val newPos = (_positionMs.value - seconds * 1000L).coerceAtLeast(0)
        seekTo(newPos)
    }

    fun playNextTrack() {
        controller?.seekToNextMediaItem()
    }

    fun playPrevTrack() {
        if (_positionMs.value > 5000L) {
            seekTo(0)
        } else {
            controller?.seekToPreviousMediaItem()
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            var lastSaveTime = 0L
            while (isActive) {
                val currentPos = controller?.currentPosition ?: 0L
                _positionMs.value = currentPos
                _durationMs.value = controller?.duration?.coerceAtLeast(0L) ?: 0L

                // Save progress every 5 seconds or if it's the first update
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSaveTime > 5000L) {
                    saveProgress()
                    lastSaveTime = currentTime
                }

                delay(500)
            }
        }
    }

    private fun saveProgress() {
        val bookId = currentBookId ?: return
        val pos = _positionMs.value
        val dur = _durationMs.value
        val trackIdx = _currentTrackIndex.value

        if (dur <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            val progress = ((pos.toDouble() / dur.toDouble()) * 100).toInt().coerceIn(0, 100)
            val locator = JSONObject().apply {
                put("trackIndex", trackIdx)
                put("positionMs", pos)
            }.toString()

            bookRepository.updateLastLocator(bookId, locator, progress)
            bookRepository.updateLastOpenedAt(bookId, System.currentTimeMillis())
        }
    }

    override fun onCleared() {
        positionUpdateJob?.cancel()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        super.onCleared()
    }

    private fun isAudioFile(name: String?): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase() ?: return false
        return ext in setOf("mp3", "m4a", "m4b", "aac", "wav")
    }

    class Factory(
        private val application: Application,
        private val bookRepository: BookRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AudiobookPlayerViewModel(application, bookRepository) as T
    }
}

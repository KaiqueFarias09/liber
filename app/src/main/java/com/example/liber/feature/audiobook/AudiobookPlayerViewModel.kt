package com.example.liber.feature.audiobook

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book
import com.example.liber.data.repository.BookRepository
import com.example.liber.feature.audiobook.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class AudiobookPlayerViewModel @Inject constructor(
    application: Application,
    private val bookRepository: BookRepository,
) : AndroidViewModel(application) {

    data class TrackInfo(val name: String, val uri: Uri, val mimeType: String? = null)

    private fun List<TrackInfo>.toJson(): String {
        val array = JSONArray()
        forEach { track ->
            array.put(JSONObject().apply {
                put("name", track.name)
                put("uri", track.uri.toString())
                track.mimeType?.let { put("mimeType", it) }
            })
        }
        return array.toString()
    }

    private fun String.toTrackInfoList(): List<TrackInfo> {
        val list = mutableListOf<TrackInfo>()
        try {
            val array = JSONArray(this)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TrackInfo(
                        obj.getString("name"),
                        obj.getString("uri").toUri(),
                        if (obj.has("mimeType")) obj.getString("mimeType") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private var currentBookId: String? = null
    private var currentBook: Book? = null

    private val _tracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val tracks: StateFlow<List<TrackInfo>> = _tracks

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val uiState: StateFlow<UiState<Unit>> = _uiState

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playWhenReady = MutableStateFlow(false)
    val playWhenReady: StateFlow<Boolean> = _playWhenReady

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _isPrepared = MutableStateFlow(false)
    val isPrepared: StateFlow<Boolean> = _isPrepared

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs

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

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    _playWhenReady.value = playWhenReady
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
                    val duration = controller.duration.coerceAtLeast(0L)
                    _durationMs.value = duration
                    if (duration > 0) {
                        saveDuration(duration)
                    }
                }
            })
            // Sync initial state
            _isPlaying.value = controller.isPlaying
            _playWhenReady.value = controller.playWhenReady
            _currentTrackIndex.value = controller.currentMediaItemIndex
            val duration = controller.duration.coerceAtLeast(0L)
            _durationMs.value = duration
            if (duration > 0) {
                saveDuration(duration)
            }
            _positionMs.value = controller.currentPosition
            _playbackSpeed.value = controller.playbackParameters.speed
            if (controller.isPlaying) startPositionUpdates()
        }, MoreExecutors.directExecutor())
    }

    private fun saveDuration(duration: Long) {
        val bookId = currentBookId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.updateDuration(bookId, duration)
        }
    }

    fun loadBook(book: Book) {
        if (currentBookId == book.id) {
            return
        }

        currentBookId = book.id
        currentBook = book

        // Reset state for new book
        _isPrepared.value = false
        _tracks.value = emptyList()
        _positionMs.value = 0L
        _durationMs.value = 0L
        _uiState.value = UiState.Loading

        loadTracks(book)
    }

    fun updateMetadataIfLoaded(book: Book) {
        val oldBook = currentBook
        if (currentBookId == book.id && oldBook != null) {
            if (oldBook.title != book.title ||
                oldBook.author != book.author ||
                oldBook.coverUri != book.coverUri ||
                oldBook.narrator != book.narrator
            ) {
                currentBook = book
                updateMetadataInController(book)
            }
        }
    }

    private fun loadTracks(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedTracks = book.tracksJson?.toTrackInfoList()
            if (!cachedTracks.isNullOrEmpty()) {
                _tracks.value = cachedTracks
                withContext(Dispatchers.Main) {
                    prepareTracks(cachedTracks, book)
                }
                return@launch
            }

            val uri = book.fileUri
            val trackList = if (uri.toString().contains("tree")) {
                val folder = DocumentFile.fromTreeUri(getApplication(), uri) ?: return@launch
                folder.listFiles()
                    .filter {
                        it.isFile && com.example.liber.data.model.AudioFormats.isSupportedFile(
                            it.name
                        )
                    }
                    .sortedBy { it.name }
                    .map {
                        TrackInfo(
                            it.name?.substringBeforeLast('.') ?: it.name ?: "Track",
                            it.uri,
                            it.type
                                ?: com.example.liber.data.model.AudioFormats.getMimeType(it.name)
                        )
                    }
            } else {
                val file = DocumentFile.fromSingleUri(getApplication(), uri) ?: return@launch
                listOf(
                    TrackInfo(
                        file.name?.substringBeforeLast('.') ?: "Track",
                        file.uri,
                        file.type
                            ?: com.example.liber.data.model.AudioFormats.getMimeType(file.name)
                    )
                )
            }

            _tracks.value = trackList
            if (trackList.isNotEmpty()) {
                bookRepository.updateTracks(book.id, trackList.toJson())
                withContext(Dispatchers.Main) {
                    prepareTracks(trackList, book)
                    _uiState.value = UiState.Success(Unit)
                }
            } else {
                _uiState.value =
                    UiState.Error(UiText.StringResource(com.example.liber.R.string.error_unknown))
            }
        }
    }

    private fun prepareTracks(trackList: List<TrackInfo>, book: Book) {
        val controller = controller ?: return

        val mediaItems = trackList.map { track ->
            MediaItem.Builder()
                .setMediaId(track.uri.toString())
                .setUri(track.uri)
                .setMimeType(track.mimeType)
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

    private fun updateMetadataInController(book: Book) {
        val controller = controller ?: return
        for (i in 0 until controller.mediaItemCount) {
            val item = controller.getMediaItemAt(i)
            val updatedMetadata = item.mediaMetadata.buildUpon()
                .setArtist(book.author)
                .setAlbumTitle(book.title)
                .setArtworkUri(book.coverUri)
                .build()
            controller.replaceMediaItem(
                i,
                item.buildUpon().setMediaMetadata(updatedMetadata).build()
            )
        }
    }

    fun togglePlayPause() {
        val controller = controller ?: return
        if (controller.playWhenReady) controller.pause() else controller.play()
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }

    private var sleepTimerJob: Job? = null

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null) {
            _sleepTimerRemainingMs.value = null
            return
        }

        val durationMs = minutes * 60 * 1000L
        _sleepTimerRemainingMs.value = durationMs

        sleepTimerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = durationMs - elapsed
                if (remaining <= 0) {
                    _sleepTimerRemainingMs.value = null
                    pause()
                    break
                }
                _sleepTimerRemainingMs.value = remaining
                delay(1000)
            }
        }
    }

    fun setSleepTimerEndOfChapter() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMs.value = -1L
    }

    fun seekTo(positionMs: Long) {
        val controller = controller ?: return
        controller.seekTo(positionMs)
        _positionMs.value = positionMs
        if (controller.playWhenReady) controller.play()
    }

    fun playTrack(index: Int) {
        val controller = controller ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekTo(index, 0)
            controller.play()
        }
    }

    fun skipForward(seconds: Int = 30) {
        val controller = controller ?: return
        val newPos = if (controller.duration > 0)
            (controller.currentPosition + seconds * 1000L).coerceAtMost(controller.duration)
        else controller.currentPosition + seconds * 1000L
        seekTo(newPos)
    }

    fun skipBackward(seconds: Int = 30) {
        val controller = controller ?: return
        seekTo((controller.currentPosition - seconds * 1000L).coerceAtLeast(0))
    }

    fun playNextTrack() {
        controller?.seekToNextMediaItem()
    }

    fun playPrevTrack() {
        if (_positionMs.value > 5000L) seekTo(0) else controller?.seekToPreviousMediaItem()
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            var lastSaveTime = 0L
            while (isActive) {
                val currentPos = controller?.currentPosition ?: 0L
                _positionMs.value = currentPos
                _durationMs.value = controller?.duration?.coerceAtLeast(0L) ?: 0L

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSaveTime > 5000L) {
                    saveProgress()
                    lastSaveTime = currentTime
                }

                if (_sleepTimerRemainingMs.value == -1L) {
                    if (currentPos >= _durationMs.value - 1000L && _durationMs.value > 0) {
                        _sleepTimerRemainingMs.value = null
                        pause()
                    }
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

            bookRepository.updateLastLocatorQuietly(bookId, locator, progress)
            bookRepository.updateLastOpenedAtQuietly(bookId, System.currentTimeMillis())
        }
    }

    override fun onCleared() {
        positionUpdateJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}

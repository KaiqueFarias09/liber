package com.example.liber.feature.audiobook

import android.app.Application
import android.content.ComponentName
import android.media.MediaMetadataRetriever
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
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.core.util.rethrowIfCancellation
import com.example.liber.data.model.Book
import com.example.liber.data.model.ReadingSessionSource
import com.example.liber.data.repository.BookRepository
import com.example.liber.data.repository.ReadingSessionTracker
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
    private val readingSessionTracker: ReadingSessionTracker,
    private val appLogger: AppLogger,
) : AndroidViewModel(application) {

    data class TrackInfo(
        val name: String,
        val uri: Uri,
        val mimeType: String? = null,
        val durationMs: Long = 0L
    )

    private fun List<TrackInfo>.toJson(): String {
        val array = JSONArray()
        forEach { track ->
            array.put(JSONObject().apply {
                put("name", track.name)
                put("uri", track.uri.toString())
                put("durationMs", track.durationMs)
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
                        if (obj.has("mimeType")) obj.getString("mimeType") else null,
                        if (obj.has("durationMs")) obj.optLong("durationMs", 0L) else 0L
                    )
                )
            }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            appLogger.warn("Failed to parse cached track list", tag = "AudiobookPlayerViewModel", throwable = e)
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
                    viewModelScope.launch {
                        if (isPlaying) {
                            currentBookId?.let { bookId ->
                                readingSessionTracker.start(
                                    channel = AUDIO_SESSION_CHANNEL,
                                    bookId = bookId,
                                    source = ReadingSessionSource.AUDIOBOOK,
                                )
                            }
                        } else {
                            readingSessionTracker.stop(AUDIO_SESSION_CHANNEL)
                        }
                    }
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
                }
            })
            // Sync initial state
            _isPlaying.value = controller.isPlaying
            _playWhenReady.value = controller.playWhenReady
            _currentTrackIndex.value = controller.currentMediaItemIndex
            val duration = controller.duration.coerceAtLeast(0L)
            _durationMs.value = duration
            _positionMs.value = controller.currentPosition
            _playbackSpeed.value = controller.playbackParameters.speed
            if (controller.isPlaying) startPositionUpdates()
        }, MoreExecutors.directExecutor())
    }

    fun loadBook(book: Book) {
        if (currentBookId == book.id) {
            return
        }

        if (currentBookId != null && _isPlaying.value) {
            viewModelScope.launch {
                readingSessionTracker.stop(AUDIO_SESSION_CHANNEL)
            }
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

    private fun getTrackDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            appLogger.warn("Failed to read track duration for $uri", tag = "AudiobookPlayerViewModel", throwable = e)
            0L
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                appLogger.warn("Failed to release MediaMetadataRetriever", tag = "AudiobookPlayerViewModel", throwable = e)
            }
        }
    }

    private fun loadTracks(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cachedTracks = book.tracksJson?.toTrackInfoList()
                if (!cachedTracks.isNullOrEmpty()) {
                    _tracks.value = cachedTracks
                    withContext(Dispatchers.Main) {
                        prepareTracks(cachedTracks, book)
                        _uiState.value = UiState.Success(Unit)
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
                            val duration = getTrackDuration(it.uri)
                            TrackInfo(
                                it.name?.substringBeforeLast('.') ?: it.name ?: "Track",
                                it.uri,
                                it.type
                                    ?: com.example.liber.data.model.AudioFormats.getMimeType(it.name),
                                durationMs = duration
                            )
                        }
                } else {
                    val file = DocumentFile.fromSingleUri(getApplication(), uri) ?: return@launch
                    val duration = getTrackDuration(file.uri)
                    listOf(
                        TrackInfo(
                            file.name?.substringBeforeLast('.') ?: "Track",
                            file.uri,
                            file.type
                                ?: com.example.liber.data.model.AudioFormats.getMimeType(file.name),
                            durationMs = duration
                        )
                    )
                }

                _tracks.value = trackList
                if (trackList.isNotEmpty()) {
                    val totalDuration = trackList.sumOf { it.durationMs }
                    bookRepository.updateTracks(book.id, trackList.toJson())
                    if (totalDuration > 0) {
                        bookRepository.updateDuration(book.id, totalDuration)
                    }
                    withContext(Dispatchers.Main) {
                        prepareTracks(trackList, book)
                        _uiState.value = UiState.Success(Unit)
                    }
                } else {
                    _uiState.value =
                        UiState.Error(UiText.StringResource(com.example.liber.R.string.error_unknown))
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                appLogger.error("Failed to load audiobook tracks", tag = "AudiobookPlayerViewModel", throwable = e)
                _uiState.value =
                    UiState.Error(UiText.DynamicString(e.message ?: "Failed to load audiobook tracks"))
            }
        }
    }

    private fun prepareTracks(trackList: List<TrackInfo>, book: Book) {
        val controller = controller ?: return

        val mediaItems = trackList.map { track ->
            MediaItem.Builder()
                .setMediaId("${book.id}|${track.uri}")
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
                e.rethrowIfCancellation()
                appLogger.warn("Failed to restore audiobook progress", tag = "AudiobookPlayerViewModel", throwable = e)
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
        val trackIdx = _currentTrackIndex.value
        val tracks = _tracks.value

        if (tracks.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalDuration = tracks.sumOf { it.durationMs }
                val currentPos = tracks.take(trackIdx).sumOf { it.durationMs } + pos

                val progress = if (totalDuration > 0) {
                    ((currentPos.toDouble() / totalDuration.toDouble()) * 100).toInt().coerceIn(0, 100)
                } else {
                    val currentTrackDur = tracks.getOrNull(trackIdx)?.durationMs ?: 0L
                    if (currentTrackDur > 0) {
                        ((pos.toDouble() / currentTrackDur.toDouble()) * 100).toInt().coerceIn(0, 100)
                    } else 0
                }

                val locator = JSONObject().apply {
                    put("trackIndex", trackIdx)
                    put("positionMs", pos)
                }.toString()

                bookRepository.updateLastLocatorQuietly(bookId, locator, progress)
                bookRepository.updateLastOpenedAtQuietly(bookId, System.currentTimeMillis())
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                appLogger.warn("Failed to persist audiobook progress", tag = "AudiobookPlayerViewModel", throwable = e)
            }
        }
    }

    override fun onCleared() {
        positionUpdateJob?.cancel()
        viewModelScope.launch {
            readingSessionTracker.stop(AUDIO_SESSION_CHANNEL)
        }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }

    private companion object {
        const val AUDIO_SESSION_CHANNEL = "audio"
    }
}

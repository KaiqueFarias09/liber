package com.example.liber.ui.reader

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.liber.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudiobookPlayerViewModel(
    application: Application,
    private val book: Book
) : AndroidViewModel(application) {

    data class TrackInfo(val name: String, val uri: Uri)

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

    private var mediaPlayer: MediaPlayer? = null
    private var positionUpdateJob: Job? = null
    private var autoPlay = false

    init {
        loadTracks()
    }

    private fun loadTracks() {
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
                    prepareTrack(0)
                }
            }
        }
    }

    private fun prepareTrack(index: Int) {
        val trackList = _tracks.value
        if (index !in trackList.indices) return

        _isPrepared.value = false
        positionUpdateJob?.cancel()

        val wasPlaying = autoPlay
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(getApplication(), trackList[index].uri)
                setOnPreparedListener { mp ->
                    _durationMs.value = mp.duration.toLong()
                    _positionMs.value = 0L
                    _currentTrackIndex.value = index
                    _isPrepared.value = true
                    if (wasPlaying) {
                        mp.start()
                        _isPlaying.value = true
                        startPositionUpdates()
                    }
                }
                setOnCompletionListener { playNextTrack() }
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePlayPause() {
        mediaPlayer ?: return
        if (!_isPrepared.value) return
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        if (!_isPrepared.value) return
        if (!_isPlaying.value) {
            player.start()
            _isPlaying.value = true
            autoPlay = true
            startPositionUpdates()
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        if (_isPlaying.value) {
            player.pause()
            _isPlaying.value = false
            positionUpdateJob?.cancel()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
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
        val nextIndex = _currentTrackIndex.value + 1
        if (nextIndex < _tracks.value.size) {
            prepareTrack(nextIndex)
        } else {
            _isPlaying.value = false
            autoPlay = false
            positionUpdateJob?.cancel()
        }
    }

    fun playPrevTrack() {
        if (_positionMs.value > 5000L) {
            seekTo(0)
        } else {
            val prevIndex = _currentTrackIndex.value - 1
            if (prevIndex >= 0) {
                prepareTrack(prevIndex)
            } else {
                seekTo(0)
            }
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                _positionMs.value = mediaPlayer?.currentPosition?.toLong() ?: 0L
                delay(500)
            }
        }
    }

    override fun onCleared() {
        positionUpdateJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }

    private fun isAudioFile(name: String?): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase() ?: return false
        return ext in setOf("mp3", "m4a", "m4b", "aac", "wav")
    }

    class Factory(
        private val application: Application,
        private val book: Book
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AudiobookPlayerViewModel(application, book) as T
    }
}

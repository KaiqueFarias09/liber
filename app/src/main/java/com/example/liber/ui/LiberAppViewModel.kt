package com.example.liber.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.liber.core.navigation.AppTab
import com.example.liber.data.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LiberAppViewModel @Inject constructor(application: Application) :
    AndroidViewModel(application) {

    private val _activeTab = MutableStateFlow(AppTab.HOME)
    val activeTab: StateFlow<AppTab> = _activeTab

    private val _activeBook = MutableStateFlow<Book?>(null)
    val activeBook: StateFlow<Book?> = _activeBook

    private val _selectedCollectionId = MutableStateFlow<Long?>(null)
    val selectedCollectionId: StateFlow<Long?> = _selectedCollectionId

    private val _libraryTabIndex = MutableStateFlow(0)
    val libraryTabIndex: StateFlow<Int> = _libraryTabIndex

    private val _isReaderOpen = MutableStateFlow(false)
    val isReaderOpen: StateFlow<Boolean> = _isReaderOpen

    fun setActiveTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun setSelectedCollectionId(id: Long?) {
        _selectedCollectionId.value = id
    }

    fun setLibraryTabIndex(index: Int) {
        _libraryTabIndex.value = index
    }

    fun openEpub(book: Book) {
        _activeBook.value = book
        _isReaderOpen.value = true
    }

    fun openAudiobook(book: Book) {
        _activeBook.value = book
        _isReaderOpen.value = true
    }

    fun openReader() {
        if (_activeBook.value != null) {
            _isReaderOpen.value = true
        }
    }

    fun closeReader() {
        _isReaderOpen.value = false
        if (_activeBook.value?.mediaType != "audio/mpeg" && _activeBook.value?.mediaType != "audiobook") {
            _activeBook.value = null
        }
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playWhenReady = MutableStateFlow(false)
    val playWhenReady: StateFlow<Boolean> = _playWhenReady

    private val _playerProgress = MutableStateFlow(0f)
    val playerProgress: StateFlow<Float> = _playerProgress

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun setPlayWhenReady(playWhenReady: Boolean) {
        _playWhenReady.value = playWhenReady
    }

    fun setPlayerProgress(progress: Float) {
        _playerProgress.value = progress
    }

    fun seekBy(seconds: Int) {
        val currentProgress = _playerProgress.value
        val duration = _activeBook.value?.durationMillis ?: 3600000L
        val seekAmount = (seconds * 1000f) / duration
        _playerProgress.value = (currentProgress + seekAmount).coerceIn(0f, 1f)
    }
}

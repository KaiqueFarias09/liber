package com.example.liber.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.liber.data.Book
import com.example.liber.ui.navigation.AppTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.publication.Publication

class LiberAppViewModel(application: Application) : AndroidViewModel(application) {

    private val _activeTab = MutableStateFlow(AppTab.HOME)
    val activeTab: StateFlow<AppTab> = _activeTab

    private val _activeBook = MutableStateFlow<Book?>(null)
    val activeBook: StateFlow<Book?> = _activeBook

    private val _activePublication = MutableStateFlow<Publication?>(null)
    val activePublication: StateFlow<Publication?> = _activePublication

    private val _selectedCollectionId = MutableStateFlow<Long?>(null)
    val selectedCollectionId: StateFlow<Long?> = _selectedCollectionId

    private val _isReaderOpen = MutableStateFlow(false)
    val isReaderOpen: StateFlow<Boolean> = _isReaderOpen

    fun setActiveTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun setSelectedCollectionId(id: Long?) {
        _selectedCollectionId.value = id
    }

    fun openEpub(book: Book, publication: Publication) {
        _activeBook.value = book
        _activePublication.value = publication
        _isReaderOpen.value = true
    }

    fun openPdf(book: Book) {
        _activeBook.value = book
        _activePublication.value = null
        _isReaderOpen.value = true
    }

    fun openReader() {
        if (_activeBook.value != null) {
            _isReaderOpen.value = true
        }
    }

    fun closeReader() {
        _isReaderOpen.value = false
        // We don't null out _activeBook here so the NowPlayingBar can stay
        // But for EPUBs/PDFs we might want to eventually? 
        // For now, let's keep it for audiobooks.
        if (_activeBook.value?.mediaType != "audio/mpeg" && _activeBook.value?.mediaType != "audiobook") {
            _activeBook.value = null
            _activePublication.value = null
        }
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playerProgress = MutableStateFlow(0f)
    val playerProgress: StateFlow<Float> = _playerProgress

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun setPlayerProgress(progress: Float) {
        _playerProgress.value = progress
    }

    fun seekBy(seconds: Int) {
        // This is a simplified implementation for the UI state.
        // In a real audiobook player, this would delegate to the actual player engine.
        // Assuming progress is 0.0 to 1.0, and we don't know the duration here, 
        // we'll just increment by a small percentage if duration is unknown, 
        // or actually calculate if duration exists in the book.
        val currentProgress = _playerProgress.value
        val duration = _activeBook.value?.durationMillis ?: 3600000L // Fallback to 1 hour
        val seekAmount = (seconds * 1000f) / duration
        _playerProgress.value = (currentProgress + seekAmount).coerceIn(0f, 1f)
    }
}

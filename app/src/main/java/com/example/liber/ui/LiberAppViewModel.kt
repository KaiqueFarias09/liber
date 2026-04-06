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

    fun setActiveTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun setSelectedCollectionId(id: Long?) {
        _selectedCollectionId.value = id
    }

    fun openEpub(book: Book, publication: Publication) {
        _activeBook.value = book
        _activePublication.value = publication
    }

    fun openPdf(book: Book) {
        _activeBook.value = book
        _activePublication.value = null
    }

    fun closeReader() {
        _activePublication.value = null
        _activeBook.value = null
    }
}

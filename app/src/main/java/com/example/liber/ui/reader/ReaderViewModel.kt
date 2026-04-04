package com.example.liber.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.Try

@OptIn(ExperimentalReadiumApi::class)
class ReaderViewModel(val publication: Publication) : ViewModel() {

    private val _showUI = MutableStateFlow(true)
    val showUI: StateFlow<Boolean> = _showUI

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<Locator>>(emptyList())
    val searchResults: StateFlow<List<Locator>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private var searchIterator: SearchIterator? = null

    fun toggleUI() {
        _showUI.value = !_showUI.value
    }

    fun search(query: String) {
        _searchQuery.value = query
        searchIterator?.close()
        searchIterator = null
        _searchResults.value = emptyList()

        if (query.isBlank()) return

        _isSearching.value = true
        viewModelScope.launch {
            val iterator = publication.search(query)
            if (iterator != null) {
                searchIterator = iterator
                loadNextResults()
            }
            _isSearching.value = false
        }
    }

    fun loadNextResults() {
        val iterator = searchIterator ?: return
        viewModelScope.launch {
            iterator.next()
                .onSuccess { result ->
                    val newLocators = result?.locators ?: emptyList()
                    _searchResults.value = _searchResults.value + newLocators
                }
                .onFailure {
                    // Handle error
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchIterator?.close()
    }

    class Factory(private val publication: Publication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderViewModel(publication) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

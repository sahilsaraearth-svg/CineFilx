package com.cinefilx.app.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinefilx.app.data.model.MediaItem
import com.cinefilx.app.data.model.MediaType
import com.cinefilx.app.data.repository.MediaRepository
import com.cinefilx.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedFilter: MediaType? = null,
    val results: List<MediaItem> = emptyList(),
    val defaultContent: List<MediaItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadDefault()
    }

    private fun loadDefault() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val trending = (repository.getTrending() as? Result.Success)?.data ?: emptyList()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                defaultContent = trending
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(results = emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.searchMedia(query)
            when (result) {
                is Result.Success -> {
                    val filtered = if (_uiState.value.selectedFilter != null) {
                        result.data.filter { it.mediaType == _uiState.value.selectedFilter }
                    } else result.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        results = filtered,
                        error = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun onFilterSelect(filter: MediaType?) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        if (_uiState.value.searchQuery.length >= 2) {
            onSearchQueryChange(_uiState.value.searchQuery)
        }
    }
}

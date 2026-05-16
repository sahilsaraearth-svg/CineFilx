package com.cinefilx.app.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinefilx.app.data.model.MediaType
import com.cinefilx.app.data.model.TmdbMovieDetail
import com.cinefilx.app.data.repository.MediaRepository
import com.cinefilx.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val detail: TmdbMovieDetail? = null,
    val mediaType: MediaType = MediaType.MOVIE,
    val error: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mediaId: Int = checkNotNull(savedStateHandle["mediaId"])
    private val mediaTypeStr: String = checkNotNull(savedStateHandle["mediaType"])

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val mediaType = MediaType.fromValue(mediaTypeStr)
            val result = when (mediaType) {
                MediaType.MOVIE -> repository.getMovieDetail(mediaId)
                MediaType.TV, MediaType.ANIME -> repository.getTvDetail(mediaId)
            }
            when (result) {
                is Result.Success -> {
                    _uiState.value = DetailUiState(
                        isLoading = false,
                        detail = result.data,
                        mediaType = mediaType
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
}

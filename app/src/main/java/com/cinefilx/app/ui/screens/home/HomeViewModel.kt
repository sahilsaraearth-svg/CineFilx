package com.cinefilx.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinefilx.app.data.model.MediaItem
import com.cinefilx.app.data.repository.MediaRepository
import com.cinefilx.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val trending: List<MediaItem> = emptyList(),
    val popularMovies: List<MediaItem> = emptyList(),
    val nowPlaying: List<MediaItem> = emptyList(),
    val popularSeries: List<MediaItem> = emptyList(),
    val popularAnime: List<MediaItem> = emptyList(),
    val topRatedAnime: List<MediaItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val trendingDeferred = async { repository.getTrending() }
                val moviesDeferred = async { repository.getPopularMovies() }
                val nowPlayingDeferred = async { repository.getNowPlayingMovies() }
                val seriesDeferred = async { repository.getPopularSeries() }
                val animeDeferred = async { repository.getPopularAnime() }
                val topAnimeDeferred = async { repository.getTopRatedAnime() }

                val trending = (trendingDeferred.await() as? Result.Success)?.data ?: emptyList()
                val movies = (moviesDeferred.await() as? Result.Success)?.data ?: emptyList()
                val nowPlaying = (nowPlayingDeferred.await() as? Result.Success)?.data ?: emptyList()
                val series = (seriesDeferred.await() as? Result.Success)?.data ?: emptyList()
                val anime = (animeDeferred.await() as? Result.Success)?.data ?: emptyList()
                val topAnime = (topAnimeDeferred.await() as? Result.Success)?.data ?: emptyList()

                _uiState.value = HomeUiState(
                    isLoading = false,
                    trending = trending,
                    popularMovies = movies,
                    nowPlaying = nowPlaying,
                    popularSeries = series,
                    popularAnime = anime,
                    topRatedAnime = topAnime
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load: ${e.message}"
                )
            }
        }
    }

    fun retry() = loadAll()
}

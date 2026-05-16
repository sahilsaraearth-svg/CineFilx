package com.cinefilx.app.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinefilx.app.data.local.WatchlistItem
import com.cinefilx.app.data.model.EztvTorrent
import com.cinefilx.app.data.model.MediaType
import com.cinefilx.app.data.model.TmdbMovieDetail
import com.cinefilx.app.data.model.TmdbSeasonDetail
import com.cinefilx.app.data.model.YtsTorrent
import com.cinefilx.app.data.repository.MediaRepository
import com.cinefilx.app.data.repository.Result
import com.cinefilx.app.data.repository.TorrentRepository
import com.cinefilx.app.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val detail: TmdbMovieDetail? = null,
    val mediaType: MediaType = MediaType.MOVIE,
    val imdbId: String? = null,
    val error: String? = null,
    // Season/episode
    val selectedSeason: Int = 1,
    val seasonDetail: TmdbSeasonDetail? = null,
    val seasonLoading: Boolean = false,
    // Torrent
    val movieTorrents: List<YtsTorrent> = emptyList(),
    val tvTorrents: List<EztvTorrent> = emptyList(),
    val torrentLoading: Boolean = false,
    val torrentError: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val torrentRepo: TorrentRepository,
    private val watchlistRepo: WatchlistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val mediaId: Int = checkNotNull(savedStateHandle["mediaId"])
    private val mediaTypeStr: String = checkNotNull(savedStateHandle["mediaType"])

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    val isInWatchlist: StateFlow<Boolean> = watchlistRepo.isInWatchlist(mediaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val mediaType = MediaType.fromValue(mediaTypeStr)
            val result = when (mediaType) {
                MediaType.MOVIE -> mediaRepo.getMovieDetail(mediaId)
                MediaType.TV, MediaType.ANIME -> mediaRepo.getTvDetail(mediaId)
            }
            when (result) {
                is Result.Success -> {
                    val detail = result.data
                    // external_ids come in append_to_response; fall back to separate call
                    val imdbId = detail.externalIds?.imdbId ?: fetchImdbId(mediaId, mediaType)
                    _uiState.value = DetailUiState(
                        isLoading = false,
                        detail = detail,
                        mediaType = mediaType,
                        imdbId = imdbId,
                        selectedSeason = 1
                    )
                    // Pre-load first season for TV
                    if (mediaType != MediaType.MOVIE) {
                        loadSeason(1)
                    }
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, error = result.message
                )
                else -> {}
            }
        }
    }

    private suspend fun fetchImdbId(id: Int, type: MediaType): String? {
        val result = if (type == MediaType.MOVIE) mediaRepo.getMovieExternalIds(id)
                     else mediaRepo.getTvExternalIds(id)
        return if (result is Result.Success) result.data.imdbId else null
    }

    fun loadSeason(seasonNumber: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(seasonLoading = true, selectedSeason = seasonNumber)
            val result = mediaRepo.getSeasonDetail(mediaId, seasonNumber)
            _uiState.value = when (result) {
                is Result.Success -> _uiState.value.copy(
                    seasonLoading = false, seasonDetail = result.data
                )
                is Result.Error -> _uiState.value.copy(seasonLoading = false)
                else -> _uiState.value.copy(seasonLoading = false)
            }
        }
    }

    fun loadTorrents() {
        val imdbId = _uiState.value.imdbId ?: return
        val mediaType = _uiState.value.mediaType
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(torrentLoading = true, torrentError = null)
            if (mediaType == MediaType.MOVIE) {
                when (val r = torrentRepo.getMovieTorrents(imdbId)) {
                    is Result.Success -> _uiState.value = _uiState.value.copy(
                        torrentLoading = false, movieTorrents = r.data
                    )
                    is Result.Error -> _uiState.value = _uiState.value.copy(
                        torrentLoading = false, torrentError = r.message
                    )
                    else -> _uiState.value = _uiState.value.copy(torrentLoading = false)
                }
            } else {
                when (val r = torrentRepo.getTvTorrents(imdbId)) {
                    is Result.Success -> _uiState.value = _uiState.value.copy(
                        torrentLoading = false, tvTorrents = r.data
                    )
                    is Result.Error -> _uiState.value = _uiState.value.copy(
                        torrentLoading = false, torrentError = r.message
                    )
                    else -> _uiState.value = _uiState.value.copy(torrentLoading = false)
                }
            }
        }
    }

    fun toggleWatchlist() {
        val detail = _uiState.value.detail ?: return
        val mediaType = _uiState.value.mediaType
        viewModelScope.launch {
            watchlistRepo.toggleWatchlist(
                WatchlistItem(
                    id         = mediaId,
                    title      = detail.displayTitle,
                    posterUrl  = detail.posterUrl(),
                    backdropUrl = detail.backdropUrl(),
                    mediaType  = mediaType.value,
                    imdbId     = _uiState.value.imdbId,
                    year       = detail.year,
                    rating     = detail.voteAverage
                )
            )
        }
    }
}

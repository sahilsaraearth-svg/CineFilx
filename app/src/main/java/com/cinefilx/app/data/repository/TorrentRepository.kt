package com.cinefilx.app.data.repository

import com.cinefilx.app.data.model.EztvTorrent
import com.cinefilx.app.data.model.YtsTorrent
import com.cinefilx.app.data.remote.EztvApiService
import com.cinefilx.app.data.remote.YtsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentRepository @Inject constructor(
    private val ytsApi: YtsApiService,
    private val eztvApi: EztvApiService
) {
    /**
     * Fetch movie torrents from YTS using full IMDB ID (e.g. "tt1234567")
     */
    suspend fun getMovieTorrents(imdbId: String): Result<List<YtsTorrent>> =
        withContext(Dispatchers.IO) {
            try {
                val response = ytsApi.getMovieTorrents(imdbId)
                if (response.isSuccessful) {
                    val torrents = response.body()?.data?.movie?.torrents ?: emptyList()
                    Result.Success(torrents)
                } else {
                    Result.Error("YTS Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }

    /**
     * Fetch TV episode torrents from EZTV using bare numeric IMDB ID (strip "tt" prefix).
     * e.g. imdbId = "tt0944947" → pass 944947
     */
    suspend fun getTvTorrents(imdbId: String): Result<List<EztvTorrent>> =
        withContext(Dispatchers.IO) {
            try {
                val numericId = imdbId.removePrefix("tt").toIntOrNull()
                    ?: return@withContext Result.Error("Invalid IMDB ID: $imdbId")
                val response = eztvApi.getTvTorrents(numericId)
                if (response.isSuccessful) {
                    val torrents = response.body()?.torrents ?: emptyList()
                    Result.Success(torrents)
                } else {
                    Result.Error("EZTV Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
}

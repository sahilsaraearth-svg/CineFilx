package com.cinefilx.app.data.repository

import com.cinefilx.app.BuildConfig
import com.cinefilx.app.data.model.MediaItem
import com.cinefilx.app.data.model.MediaType
import com.cinefilx.app.data.model.TmdbExternalIds
import com.cinefilx.app.data.model.TmdbMovieDetail
import com.cinefilx.app.data.model.TmdbSeasonDetail
import com.cinefilx.app.data.model.toMediaItem
import com.cinefilx.app.data.remote.TmdbApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

@Singleton
class MediaRepository @Inject constructor(
    private val api: TmdbApiService
) {
    private val apiKey = BuildConfig.TMDB_API_KEY

    suspend fun getTrending(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getTrendingAll(apiKey)
            if (response.isSuccessful) {
                val items = response.body()?.results?.mapNotNull { movie ->
                    val type = when (movie.mediaType) {
                        "tv" -> if (movie.genreIds.contains(16)) MediaType.ANIME else MediaType.TV
                        else -> MediaType.MOVIE
                    }
                    movie.toMediaItem(type)
                } ?: emptyList()
                Result.Success(items)
            } else {
                Result.Error("API Error: ${response.code()}")
            }
        }
    }

    suspend fun getPopularMovies(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getPopularMovies(apiKey, page)
            if (response.isSuccessful) {
                Result.Success(response.body()?.results?.map { it.toMediaItem(MediaType.MOVIE) } ?: emptyList())
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    suspend fun getTopRatedMovies(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getTopRatedMovies(apiKey, page)
            if (response.isSuccessful) {
                Result.Success(response.body()?.results?.map { it.toMediaItem(MediaType.MOVIE) } ?: emptyList())
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    suspend fun getNowPlayingMovies(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getNowPlayingMovies(apiKey)
            if (response.isSuccessful) {
                Result.Success(response.body()?.results?.map { it.toMediaItem(MediaType.MOVIE) } ?: emptyList())
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    suspend fun getPopularSeries(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getPopularTv(apiKey, page)
            if (response.isSuccessful) {
                Result.Success(response.body()?.results?.map { it.toMediaItem(MediaType.TV) } ?: emptyList())
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    suspend fun getTopRatedSeries(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getTopRatedTv(apiKey)
            if (response.isSuccessful) {
                Result.Success(response.body()?.results?.map { it.toMediaItem(MediaType.TV) } ?: emptyList())
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    suspend fun getPopularAnime(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getAnime(apiKey, page = page)
            if (response.isSuccessful) {
                Result.Success(response.body()?.results?.map { it.toMediaItem(MediaType.ANIME) } ?: emptyList())
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    suspend fun getTopRatedAnime(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getTopRatedAnime(apiKey)
            if (response.isSuccessful) {
                Result.Success(response.body()?.results?.map { it.toMediaItem(MediaType.ANIME) } ?: emptyList())
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    suspend fun getMovieDetail(id: Int): Result<TmdbMovieDetail> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getMovieDetail(id, apiKey)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    suspend fun getTvDetail(id: Int): Result<TmdbMovieDetail> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getTvDetail(id, apiKey)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    suspend fun getMovieExternalIds(id: Int): Result<TmdbExternalIds> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getMovieExternalIds(id, apiKey)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    suspend fun getTvExternalIds(id: Int): Result<TmdbExternalIds> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.getTvExternalIds(id, apiKey)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    suspend fun getSeasonDetail(tvId: Int, seasonNumber: Int): Result<TmdbSeasonDetail> =
        withContext(Dispatchers.IO) {
            safeApiCall {
                val response = api.getSeasonDetail(tvId, seasonNumber, apiKey)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else Result.Error("API Error: ${response.code()}")
            }
        }

    suspend fun searchMedia(query: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.searchMulti(apiKey, query)
            if (response.isSuccessful) {
                val items = response.body()?.results
                    ?.filter { it.mediaType != "person" }
                    ?.mapNotNull { movie ->
                        val type = when (movie.mediaType) {
                            "tv" -> if (movie.genreIds.contains(16)) MediaType.ANIME else MediaType.TV
                            else -> MediaType.MOVIE
                        }
                        movie.toMediaItem(type)
                    } ?: emptyList()
                Result.Success(items)
            } else Result.Error("API Error: ${response.code()}")
        }
    }

    private inline fun <T> safeApiCall(block: () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}", e)
        }
    }
}

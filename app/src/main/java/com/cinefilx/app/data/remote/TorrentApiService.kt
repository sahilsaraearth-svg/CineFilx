package com.cinefilx.app.data.remote

import com.cinefilx.app.data.model.EztvResponse
import com.cinefilx.app.data.model.YtsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * YTS movie torrent API — base URL: https://yts.mx/api/v2/
 */
interface YtsApiService {
    @GET("movie_details.json")
    suspend fun getMovieTorrents(
        @Query("imdb_id") imdbId: String,
        @Query("with_images") withImages: Boolean = false,
        @Query("with_cast") withCast: Boolean = false
    ): Response<YtsResponse>
}

/**
 * EZTV TV torrent API — base URL: https://eztv.re/api/
 * imdb_id must be a bare number (no "tt" prefix, no leading zeros)
 */
interface EztvApiService {
    @GET("get-torrents")
    suspend fun getTvTorrents(
        @Query("imdb_id") imdbId: Int,
        @Query("limit") limit: Int = 100
    ): Response<EztvResponse>
}

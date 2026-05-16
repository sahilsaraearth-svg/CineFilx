package com.cinefilx.app.data.remote

import com.cinefilx.app.data.model.TmdbMovieDetail
import com.cinefilx.app.data.model.TmdbMovieResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    // Trending
    @GET("trending/all/week")
    suspend fun getTrendingAll(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    // Movies
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "credits,videos,similar"
    ): Response<TmdbMovieDetail>

    // TV Series
    @GET("tv/popular")
    suspend fun getPopularTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    @GET("tv/top_rated")
    suspend fun getTopRatedTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    @GET("tv/on_the_air")
    suspend fun getOnAirTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    @GET("tv/{tv_id}")
    suspend fun getTvDetail(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "credits,videos,similar"
    ): Response<TmdbMovieDetail>

    // Anime (TV with genre 16 = Animation, or keyword search)
    @GET("discover/tv")
    suspend fun getAnime(
        @Query("api_key") apiKey: String,
        @Query("with_genres") withGenres: String = "16",
        @Query("with_origin_country") withOriginCountry: String = "JP",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    @GET("discover/tv")
    suspend fun getTopRatedAnime(
        @Query("api_key") apiKey: String,
        @Query("with_genres") withGenres: String = "16",
        @Query("with_origin_country") withOriginCountry: String = "JP",
        @Query("sort_by") sortBy: String = "vote_average.desc",
        @Query("vote_count.gte") minVoteCount: Int = 200,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    // Search
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>

    @GET("search/tv")
    suspend fun searchTv(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): Response<TmdbMovieResponse>
}

package com.cinefilx.app.data.model

import com.google.gson.annotations.SerializedName

// TMDB API response models

data class TmdbMovieResponse(
    @SerializedName("page") val page: Int,
    @SerializedName("results") val results: List<TmdbMovie>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class TmdbMovie(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String? = null,
    @SerializedName("name") val name: String? = null, // for TV/Anime
    @SerializedName("original_title") val originalTitle: String? = null,
    @SerializedName("original_name") val originalName: String? = null,
    @SerializedName("overview") val overview: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("genre_ids") val genreIds: List<Int>,
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("popularity") val popularity: Double
) {
    val displayTitle: String get() = title ?: name ?: originalTitle ?: originalName ?: "Unknown"
    val displayDate: String get() = releaseDate ?: firstAirDate ?: ""
    val year: String get() = displayDate.take(4)
    fun posterUrl(size: String = "w500"): String =
        if (posterPath != null) "https://image.tmdb.org/t/p/$size$posterPath" else ""
    fun backdropUrl(size: String = "w1280"): String =
        if (backdropPath != null) "https://image.tmdb.org/t/p/$size$backdropPath" else ""
}

data class TmdbMovieDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("overview") val overview: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("runtime") val runtime: Int? = null,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>? = null,
    @SerializedName("genres") val genres: List<TmdbGenre>,
    @SerializedName("status") val status: String,
    @SerializedName("tagline") val tagline: String?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerializedName("production_companies") val productionCompanies: List<TmdbCompany>,
    @SerializedName("spoken_languages") val spokenLanguages: List<TmdbLanguage>,
    @SerializedName("videos") val videos: TmdbVideosWrapper? = null,
    @SerializedName("credits") val credits: TmdbCredits? = null,
    @SerializedName("similar") val similar: TmdbMovieResponse? = null
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val displayDate: String get() = releaseDate ?: firstAirDate ?: ""
    val year: String get() = displayDate.take(4)
    val displayRuntime: String get() {
        val rt = runtime ?: episodeRunTime?.firstOrNull() ?: 0
        return if (rt > 0) "${rt}m" else ""
    }
    fun posterUrl(size: String = "w500"): String =
        if (posterPath != null) "https://image.tmdb.org/t/p/$size$posterPath" else ""
    fun backdropUrl(size: String = "w1280"): String =
        if (backdropPath != null) "https://image.tmdb.org/t/p/$size$backdropPath" else ""
}

data class TmdbGenre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class TmdbCompany(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class TmdbLanguage(
    @SerializedName("english_name") val englishName: String,
    @SerializedName("iso_639_1") val iso: String,
    @SerializedName("name") val name: String
)

data class TmdbVideosWrapper(
    @SerializedName("results") val results: List<TmdbVideo>
)

data class TmdbVideo(
    @SerializedName("id") val id: String,
    @SerializedName("key") val key: String,
    @SerializedName("name") val name: String,
    @SerializedName("site") val site: String,
    @SerializedName("type") val type: String,
    @SerializedName("official") val official: Boolean
) {
    val youtubeUrl: String get() = "https://www.youtube.com/watch?v=$key"
    val youtubeThumbnail: String get() = "https://img.youtube.com/vi/$key/hqdefault.jpg"
}

data class TmdbCredits(
    @SerializedName("cast") val cast: List<TmdbCast>,
    @SerializedName("crew") val crew: List<TmdbCrew>
)

data class TmdbCast(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("character") val character: String,
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("order") val order: Int
) {
    fun profileUrl(): String =
        if (profilePath != null) "https://image.tmdb.org/t/p/w185$profilePath" else ""
}

data class TmdbCrew(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("job") val job: String,
    @SerializedName("department") val department: String,
    @SerializedName("profile_path") val profilePath: String?
)

// Media type enum for navigation/filtering
enum class MediaType(val value: String, val label: String) {
    MOVIE("movie", "Movies"),
    TV("tv", "Series"),
    ANIME("anime", "Anime");

    companion object {
        fun fromValue(value: String): MediaType = entries.find { it.value == value } ?: MOVIE
    }
}

// UI wrapper combining TMDB data with play sources
data class MediaItem(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String,
    val backdropUrl: String,
    val rating: Double,
    val year: String,
    val mediaType: MediaType,
    val genres: List<String> = emptyList()
)

fun TmdbMovie.toMediaItem(type: MediaType): MediaItem = MediaItem(
    id = id,
    title = displayTitle,
    overview = overview,
    posterUrl = posterUrl(),
    backdropUrl = backdropUrl(),
    rating = voteAverage,
    year = year,
    mediaType = type
)

package com.cinefilx.app.data.model

import com.google.gson.annotations.SerializedName

data class TmdbMovieResponse(
    @SerializedName("page") val page: Int,
    @SerializedName("results") val results: List<TmdbMovie>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class TmdbMovie(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("original_title") val originalTitle: String? = null,
    @SerializedName("original_name") val originalName: String? = null,
    @SerializedName("overview") val overview: String = "",
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("popularity") val popularity: Double = 0.0,
    @SerializedName("origin_country") val originCountry: List<String> = emptyList()
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
    @SerializedName("overview") val overview: String = "",
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("runtime") val runtime: Int? = null,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>? = null,
    @SerializedName("genres") val genres: List<TmdbGenre> = emptyList(),
    @SerializedName("status") val status: String = "",
    @SerializedName("tagline") val tagline: String? = null,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerializedName("seasons") val seasons: List<TmdbSeason>? = null,
    @SerializedName("production_companies") val productionCompanies: List<TmdbCompany> = emptyList(),
    @SerializedName("spoken_languages") val spokenLanguages: List<TmdbLanguage> = emptyList(),
    @SerializedName("videos") val videos: TmdbVideosWrapper? = null,
    @SerializedName("credits") val credits: TmdbCredits? = null,
    @SerializedName("similar") val similar: TmdbMovieResponse? = null,
    @SerializedName("external_ids") val externalIds: TmdbExternalIds? = null
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

data class TmdbExternalIds(
    @SerializedName("imdb_id") val imdbId: String? = null,
    @SerializedName("tvdb_id") val tvdbId: Int? = null
)

data class TmdbSeason(
    @SerializedName("id") val id: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("name") val name: String,
    @SerializedName("episode_count") val episodeCount: Int,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("air_date") val airDate: String? = null,
    @SerializedName("overview") val overview: String = ""
) {
    fun posterUrl(): String =
        if (posterPath != null) "https://image.tmdb.org/t/p/w300$posterPath" else ""
}

data class TmdbSeasonDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("name") val name: String,
    @SerializedName("episodes") val episodes: List<TmdbEpisode> = emptyList()
)

data class TmdbEpisode(
    @SerializedName("id") val id: Int,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("name") val name: String,
    @SerializedName("overview") val overview: String = "",
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("air_date") val airDate: String? = null,
    @SerializedName("runtime") val runtime: Int? = null
) {
    fun stillUrl(): String =
        if (stillPath != null) "https://image.tmdb.org/t/p/w300$stillPath" else ""
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
    @SerializedName("official") val official: Boolean = false
) {
    val youtubeUrl: String get() = "https://www.youtube.com/watch?v=$key"
    val youtubeThumbnail: String get() = "https://img.youtube.com/vi/$key/hqdefault.jpg"
}

data class TmdbCredits(
    @SerializedName("cast") val cast: List<TmdbCast> = emptyList(),
    @SerializedName("crew") val crew: List<TmdbCrew> = emptyList()
)

data class TmdbCast(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("character") val character: String = "",
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("order") val order: Int = 0
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

enum class MediaType(val value: String, val label: String) {
    MOVIE("movie", "Movies"),
    TV("tv", "Series"),
    ANIME("anime", "Anime");

    companion object {
        fun fromValue(value: String): MediaType = entries.find { it.value == value } ?: MOVIE
    }
}

data class MediaItem(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String,
    val backdropUrl: String,
    val rating: Double,
    val year: String,
    val mediaType: MediaType,
    val genres: List<String> = emptyList(),
    val imdbId: String? = null
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

// ─── Torrent Models ───────────────────────────────────────────────────────────

data class YtsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: YtsData
)

data class YtsData(
    @SerializedName("movie") val movie: YtsMovie? = null,
    @SerializedName("movies") val movies: List<YtsMovie>? = null
)

data class YtsMovie(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("imdb_code") val imdbCode: String,
    @SerializedName("torrents") val torrents: List<YtsTorrent> = emptyList()
)

data class YtsTorrent(
    @SerializedName("url") val url: String,
    @SerializedName("hash") val hash: String,
    @SerializedName("quality") val quality: String,
    @SerializedName("type") val type: String,
    @SerializedName("seeds") val seeds: Int,
    @SerializedName("peers") val peers: Int,
    @SerializedName("size") val size: String
) {
    val magnetUrl: String get() =
        "magnet:?xt=urn:btih:$hash&dn=${title}&tr=udp://tracker.opentrackr.org:1337/announce&tr=udp://open.stealth.si:80/announce"
    private val title: String get() = ""
}

data class EztvResponse(
    @SerializedName("imdb_id") val imdbId: String,
    @SerializedName("torrents_count") val torrentsCount: Int,
    @SerializedName("torrents") val torrents: List<EztvTorrent> = emptyList()
)

data class EztvTorrent(
    @SerializedName("id") val id: Int,
    @SerializedName("hash") val hash: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("magnet_url") val magnetUrl: String,
    @SerializedName("title") val title: String,
    @SerializedName("imdb_id") val imdbId: String,
    @SerializedName("season") val season: String = "",
    @SerializedName("episode") val episode: String = "",
    @SerializedName("seeds") val seeds: Int = 0,
    @SerializedName("peers") val peers: Int = 0,
    @SerializedName("size_bytes") val sizeBytes: Long = 0L
) {
    val displaySize: String get() {
        val mb = sizeBytes / 1024 / 1024
        return if (mb > 1024) "%.1f GB".format(mb / 1024.0) else "$mb MB"
    }
}

package com.cinefilx.app.data.repository

import com.cinefilx.app.data.model.EztvTorrent
import com.cinefilx.app.data.model.YtsTorrent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Torrent repository using apibay.org (The Pirate Bay API).
 * cat=201 = Movies, cat=205 = TV Shows
 */
@Singleton
class TorrentRepository @Inject constructor() {

    private val trackers = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.tracker.cl:1337/announce",
        "udp://tracker.openbittorrent.com:80",
        "udp://tracker.internetwarriors.net:1337/announce",
        "udp://9.rarbg.to:2710/announce"
    )

    /**
     * Movie torrents from apibay.org. Searches by title + year.
     */
    suspend fun getMovieTorrents(title: String, year: String): Result<List<YtsTorrent>> =
        withContext(Dispatchers.IO) {
            try {
                val query = URLEncoder.encode("$title $year", "UTF-8")
                val url = "https://apibay.org/q.php?q=$query&cat=201"
                val json = fetchUrl(url)
                val arr = JSONArray(json)
                if (arr.length() == 0) return@withContext Result.Success(emptyList())

                val list = mutableListOf<YtsTorrent>()
                for (i in 0 until arr.length()) {
                    val t = arr.getJSONObject(i)
                    val hash = t.optString("info_hash", "")
                    val name = t.optString("name", "")
                    val seeds = t.optInt("seeders", 0)
                    val peers = t.optInt("leechers", 0)
                    val size = t.optLong("size", 0)
                    // Skip dummy "No results" entry that apibay returns
                    if (hash.isEmpty() || hash == "0000000000000000000000000000000000000000") continue
                    list.add(
                        YtsTorrent(
                            hash = hash,
                            quality = extractQuality(name),
                            type = "web",
                            size = formatSize(size),
                            seeds = seeds,
                            peers = peers,
                            magnetUrl = buildMagnet(hash, name)
                        )
                    )
                }

                // Sort by seeders desc, return top 8
                val sorted = list.sortedByDescending { it.seeds }.take(8)
                Result.Success(sorted)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Torrent search failed")
            }
        }

    /**
     * TV torrents from apibay.org. Searches by title + SxxExx.
     */
    suspend fun getTvTorrents(title: String, season: Int, episode: Int): Result<List<EztvTorrent>> =
        withContext(Dispatchers.IO) {
            try {
                val episodeStr = "S%02dE%02d".format(season, episode)
                val query = URLEncoder.encode("$title $episodeStr", "UTF-8")
                val url = "https://apibay.org/q.php?q=$query&cat=205"
                val json = fetchUrl(url)
                val arr = JSONArray(json)
                if (arr.length() == 0) return@withContext Result.Success(emptyList())

                val list = mutableListOf<EztvTorrent>()
                for (i in 0 until arr.length()) {
                    val t = arr.getJSONObject(i)
                    val hash = t.optString("info_hash", "")
                    val name = t.optString("name", "")
                    val seeds = t.optInt("seeders", 0)
                    val peers = t.optInt("leechers", 0)
                    val size = t.optLong("size", 0)
                    if (hash.isEmpty() || hash == "0000000000000000000000000000000000000000") continue
                    list.add(
                        EztvTorrent(
                            id = i,
                            hash = hash,
                            filename = name,
                            title = name,
                            imdbId = "",
                            seeds = seeds,
                            peers = peers,
                            sizeBytes = size,
                            magnetUrl = buildMagnet(hash, name)
                        )
                    )
                }

                val sorted = list.sortedByDescending { it.seeds }.take(8)
                Result.Success(sorted)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Torrent search failed")
            }
        }

    private fun buildMagnet(hash: String, name: String): String {
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val trackerString = trackers.joinToString("&") {
            "tr=${URLEncoder.encode(it, "UTF-8")}"
        }
        return "magnet:?xt=urn:btih:$hash&dn=$encodedName&$trackerString"
    }

    private fun extractQuality(name: String): String {
        return when {
            name.contains("2160p", ignoreCase = true) || name.contains("4K", ignoreCase = true) -> "4K"
            name.contains("1080p", ignoreCase = true) -> "1080p"
            name.contains("720p", ignoreCase = true) -> "720p"
            name.contains("480p", ignoreCase = true) -> "480p"
            else -> "HD"
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "?"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        val mb = bytes / (1024.0 * 1024.0)
        return if (gb >= 1.0) "%.2f GB".format(gb) else "%.0f MB".format(mb)
    }

    private fun fetchUrl(urlStr: String): String {
        val url = java.net.URL(urlStr)
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36"
        )
        conn.instanceFollowRedirects = true
        return conn.inputStream.bufferedReader().readText()
    }
}

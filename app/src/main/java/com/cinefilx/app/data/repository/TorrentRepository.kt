package com.cinefilx.app.data.repository

import com.cinefilx.app.data.model.EztvTorrent
import com.cinefilx.app.data.model.YtsTorrent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Torrent repository using direct HTTP calls (no Retrofit) so we can
 * easily try multiple mirror URLs when one fails DNS.
 */
@Singleton
class TorrentRepository @Inject constructor() {

    // ── YTS mirrors (tried in order) ─────────────────────────────────────────
    private val ytsMirrors = listOf(
        "https://yts.mx/api/v2",
        "https://yts.pm/api/v2",
        "https://yts.lt/api/v2",
        "https://yts.rs/api/v2"
    )

    // ── EZTV mirrors ─────────────────────────────────────────────────────────
    private val eztvMirrors = listOf(
        "https://eztv.re/api",
        "https://eztv.wf/api",
        "https://eztvx.to/api",
        "https://eztv.tf/api"
    )

    /**
     * Movie torrents from YTS. imdbId = full "tt1234567" format.
     */
    suspend fun getMovieTorrents(imdbId: String): Result<List<YtsTorrent>> =
        withContext(Dispatchers.IO) {
            var lastError = "All mirrors failed"
            for (mirror in ytsMirrors) {
                try {
                    val url = "$mirror/movie_details.json?imdb_id=$imdbId&with_images=false&with_cast=false"
                    val json = URL(url).readText(connectTimeout = 8000, readTimeout = 8000)
                    val root = JSONObject(json)
                    val status = root.optString("status", "")
                    if (status != "ok") {
                        lastError = root.optString("status_message", "Unknown error")
                        continue
                    }
                    val movieObj = root.optJSONObject("data")?.optJSONObject("movie")
                        ?: return@withContext Result.Success(emptyList())
                    val torrentsArr = movieObj.optJSONArray("torrents")
                        ?: return@withContext Result.Success(emptyList())
                    val list = mutableListOf<YtsTorrent>()
                    for (i in 0 until torrentsArr.length()) {
                        val t = torrentsArr.getJSONObject(i)
                        val hash = t.optString("hash", "")
                        val quality = t.optString("quality", "?")
                        val type = t.optString("type", "")
                        val size = t.optString("size", "?")
                        val seeds = t.optInt("seeds", 0)
                        val peers = t.optInt("peers", 0)
                        if (hash.isNotEmpty()) {
                            val encodedTitle = movieObj.optString("title_long", imdbId)
                                .replace(" ", "%20")
                            val magnet = buildMagnet(hash, encodedTitle)
                            list.add(YtsTorrent(
                                hash = hash,
                                quality = quality,
                                type = type,
                                size = size,
                                seeds = seeds,
                                peers = peers,
                                magnetUrl = magnet
                            ))
                        }
                    }
                    return@withContext Result.Success(list)
                } catch (e: Exception) {
                    lastError = e.message ?: "Error"
                    continue
                }
            }
            Result.Error(lastError)
        }

    /**
     * TV torrents from EZTV. imdbId = full "tt1234567" format.
     * EZTV needs bare numeric id (strip tt prefix).
     */
    suspend fun getTvTorrents(imdbId: String): Result<List<EztvTorrent>> =
        withContext(Dispatchers.IO) {
            val numericId = imdbId.removePrefix("tt").trimStart('0').ifEmpty { "0" }
            var lastError = "All mirrors failed"
            for (mirror in eztvMirrors) {
                try {
                    val url = "$mirror/get-torrents?imdb_id=$numericId&limit=100"
                    val json = URL(url).readText(connectTimeout = 8000, readTimeout = 8000)
                    val root = JSONObject(json)
                    val count = root.optInt("torrents_count", 0)
                    if (count == 0) return@withContext Result.Success(emptyList())
                    val torrentsArr = root.optJSONArray("torrents")
                        ?: return@withContext Result.Success(emptyList())
                    val list = mutableListOf<EztvTorrent>()
                    for (i in 0 until torrentsArr.length()) {
                        val t = torrentsArr.getJSONObject(i)
                        val magnet = t.optString("magnet_url", "")
                        if (magnet.isEmpty()) continue
                        list.add(EztvTorrent(
                            id = t.optInt("id", 0),
                            hash = t.optString("hash", ""),
                            filename = t.optString("filename", ""),
                            title = t.optString("title", ""),
                            imdbId = t.optString("imdb_id", ""),
                            seeds = t.optInt("seeds", 0),
                            peers = t.optInt("peers", 0),
                            sizeBytes = t.optLong("size_bytes", 0),
                            magnetUrl = magnet
                        ))
                    }
                    return@withContext Result.Success(list)
                } catch (e: Exception) {
                    lastError = e.message ?: "Error"
                    continue
                }
            }
            Result.Error(lastError)
        }

    private fun buildMagnet(hash: String, encodedTitle: String): String {
        val trackers = listOf(
            "udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce",
            "udp%3A%2F%2Fopen.tracker.cl%3A1337%2Fannounce",
            "udp%3A%2F%2F9.rarbg.to%3A2710%2Fannounce",
            "udp%3A%2F%2Ftracker.openbittorrent.com%3A80",
            "udp%3A%2F%2Ftracker.internetwarriors.net%3A1337%2Fannounce"
        )
        val trackerString = trackers.joinToString("&") { "tr=$it" }
        return "magnet:?xt=urn:btih:$hash&dn=$encodedTitle&$trackerString"
    }

    private fun URL.readText(connectTimeout: Int, readTimeout: Int): String {
        val conn = openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = connectTimeout
        conn.readTimeout = readTimeout
        conn.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36"
        )
        conn.instanceFollowRedirects = true
        return conn.inputStream.bufferedReader().readText()
    }
}

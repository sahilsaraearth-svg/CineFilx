package com.cinefilx.app.ui.screens.player

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowInsetsController
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cinefilx.app.ui.theme.CineFilxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        hideSystemUI()

        val tmdbId    = intent.getIntExtra(EXTRA_TMDB_ID, 0)
        val imdbId    = intent.getStringExtra(EXTRA_IMDB_ID) ?: ""
        val mediaType = intent.getStringExtra(EXTRA_MEDIA_TYPE) ?: "movie"
        val season    = intent.getIntExtra(EXTRA_SEASON, 1)
        val episode   = intent.getIntExtra(EXTRA_EPISODE, 1)
        val title     = intent.getStringExtra(EXTRA_TITLE) ?: "Now Playing"

        setContent {
            CineFilxTheme {
                PlayerScreen(
                    tmdbId    = tmdbId,
                    imdbId    = imdbId,
                    mediaType = mediaType,
                    season    = season,
                    episode   = episode,
                    title     = title,
                    onBack    = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(android.view.WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                enterPictureInPictureMode(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                )
            }
        }
    }

    companion object {
        const val EXTRA_TMDB_ID    = "TMDB_ID"
        const val EXTRA_IMDB_ID    = "IMDB_ID"
        const val EXTRA_MEDIA_TYPE = "MEDIA_TYPE"
        const val EXTRA_SEASON     = "SEASON"
        const val EXTRA_EPISODE    = "EPISODE"
        const val EXTRA_TITLE      = "TITLE"
    }
}

// ─── Stream servers ───────────────────────────────────────────────────────────

data class StreamServer(val name: String, val tag: String)

private val SERVERS = listOf(
    StreamServer("VidSrc",    "vidsrc"),     // vidsrc.to  - huge library
    StreamServer("VidSrc.me", "vidsrcme"),   // vidsrcme.ru - very reliable
    StreamServer("2Embed",    "2embed"),     // 2embed.cc
    StreamServer("VidBinge",  "vidbinge"),   // vidbinge.dev
    StreamServer("VidSrc.icu","vidsrcicu"),  // vidsrc.icu
    StreamServer("Videasy",   "videasy")     // player.videasy.net
)

private fun buildEmbedUrl(
    server: String,
    tmdbId: Int,
    imdbId: String,
    mediaType: String,
    season: Int,
    episode: Int
): String {
    val isMovie = mediaType == "movie"
    val hasImdb = imdbId.startsWith("tt") && imdbId.length > 4

    return when (server) {
        // vidsrc.to: supports both imdb and tmdb
        "vidsrc" -> if (isMovie)
            if (hasImdb) "https://vidsrc.to/embed/movie/$imdbId"
            else         "https://vidsrc.to/embed/movie/$tmdbId"
        else
            if (hasImdb) "https://vidsrc.to/embed/tv/$imdbId/$season/$episode"
            else         "https://vidsrc.to/embed/tv/$tmdbId/$season/$episode"

        // vidsrc.me (redirects to vidsrcme.ru) - tmdb only
        "vidsrcme" -> if (isMovie)
            "https://vidsrcme.ru/embed/movie?tmdb=$tmdbId"
        else
            "https://vidsrcme.ru/embed/tv?tmdb=$tmdbId&season=$season&episode=$episode"

        // 2embed.cc - supports imdb and tmdb
        "2embed" -> if (isMovie)
            if (hasImdb) "https://www.2embed.cc/embed/$imdbId"
            else         "https://www.2embed.cc/embed/$tmdbId"
        else
            if (hasImdb) "https://www.2embed.cc/embedtv/$imdbId&s=$season&e=$episode"
            else         "https://www.2embed.cc/embedtv/$tmdbId&s=$season&e=$episode"

        // vidbinge.dev - tmdb only
        "vidbinge" -> if (isMovie)
            "https://vidbinge.dev/embed/movie/$tmdbId"
        else
            "https://vidbinge.dev/embed/tv/$tmdbId/$season/$episode"

        // vidsrc.icu - tmdb only
        "vidsrcicu" -> if (isMovie)
            "https://vidsrc.icu/embed/movie/$tmdbId"
        else
            "https://vidsrc.icu/embed/tv/$tmdbId/$season/$episode"

        // player.videasy.net - tmdb only
        "videasy" -> if (isMovie)
            "https://player.videasy.net/movie/$tmdbId"
        else
            "https://player.videasy.net/tv/$tmdbId/$season/$episode"

        else -> "about:blank"
    }
}

// ─── Player composable ────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    tmdbId: Int,
    imdbId: String,
    mediaType: String,
    season: Int,
    episode: Int,
    title: String,
    onBack: () -> Unit
) {
    var selectedServer by remember { mutableStateOf(0) }
    var showControls   by remember { mutableStateOf(true) }
    var webViewRef     by remember { mutableStateOf<WebView?>(null) }
    var isLoading      by remember { mutableStateOf(true) }

    val embedUrl = remember(selectedServer, tmdbId, imdbId, mediaType, season, episode) {
        buildEmbedUrl(
            server    = SERVERS[selectedServer].tag,
            tmdbId    = tmdbId,
            imdbId    = imdbId,
            mediaType = mediaType,
            season    = season,
            episode   = episode
        )
    }

    LaunchedEffect(embedUrl) {
        isLoading = true
    }

    // Auto-hide controls after 4s
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { showControls = !showControls }
    ) {
        // ── WebView ──────────────────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    settings.apply {
                        javaScriptEnabled          = true
                        domStorageEnabled          = true
                        mixedContentMode           = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        mediaPlaybackRequiresUserGesture = false
                        userAgentString            =
                            "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/124.0.6367.82 Mobile Safari/537.36"
                        allowFileAccess            = true
                        setSupportZoom(false)
                        builtInZoomControls        = false
                        displayZoomControls        = false
                        loadWithOverviewMode       = true
                        useWideViewPort            = true
                        cacheMode                  = WebSettings.LOAD_DEFAULT
                        allowContentAccess         = true
                        databaseEnabled            = true
                    }
                    webChromeClient = object : WebChromeClient() {}
                    webViewClient   = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            isLoading = false
                        }
                        // Never override URL loading — let WebView handle all redirects
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: android.webkit.WebResourceRequest
                        ): Boolean = false
                    }
                    loadUrl(embedUrl)
                }
            },
            update = { wv ->
                val cur = wv.url ?: ""
                // Only reload if the server changed significantly
                if (!cur.contains(SERVERS[selectedServer].tag.take(6)) &&
                    !cur.startsWith(embedUrl.take(25))
                ) {
                    isLoading = true
                    wv.loadUrl(embedUrl)
                }
            }
        )

        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFE50914),
                strokeWidth = 3.dp
            )
        }

        // ── Controls overlay ─────────────────────────────────────────────────
        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.75f))
            ) {
                // Title + back + reload
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = if (mediaType == "movie") title
                               else "$title · S${season}E${episode}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    IconButton(onClick = {
                        isLoading = true
                        webViewRef?.reload()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
                    }
                }

                // Server tabs — horizontal scroll so all fit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SERVERS.forEachIndexed { index, server ->
                        val selected = index == selectedServer
                        Surface(
                            modifier = Modifier.clickable {
                                if (selectedServer != index) {
                                    selectedServer = index
                                    isLoading = true
                                    // Force load new URL
                                    val newUrl = buildEmbedUrl(
                                        server    = SERVERS[index].tag,
                                        tmdbId    = tmdbId,
                                        imdbId    = imdbId,
                                        mediaType = mediaType,
                                        season    = season,
                                        episode   = episode
                                    )
                                    webViewRef?.loadUrl(newUrl)
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) Color(0xFFE50914)
                                    else Color.White.copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = server.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

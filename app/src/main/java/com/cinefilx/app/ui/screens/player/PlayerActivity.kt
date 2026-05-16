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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

// ─── Stream server definitions ────────────────────────────────────────────────

data class StreamServer(val name: String, val tag: String)

private val SERVERS = listOf(
    StreamServer("VidSrc",     "vidsrc"),
    StreamServer("2Embed",     "2embed"),
    StreamServer("VidSrc.me",  "vidsrcme"),
    StreamServer("SuperEmbed", "superembed"),
    StreamServer("AutoEmbed",  "autoembed")
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
    // Use imdbId if available, fallback to tmdbId
    val hasImdb = imdbId.startsWith("tt") && imdbId.length > 4
    return when (server) {
        "vidsrc" -> if (isMovie) {
            if (hasImdb) "https://vidsrc.to/embed/movie/$imdbId"
            else "https://vidsrc.to/embed/movie/$tmdbId"
        } else {
            if (hasImdb) "https://vidsrc.to/embed/tv/$imdbId/$season/$episode"
            else "https://vidsrc.to/embed/tv/$tmdbId/$season/$episode"
        }

        "vidsrcme" -> if (isMovie)
            "https://vidsrcme.ru/embed/movie?tmdb=$tmdbId"
        else
            "https://vidsrcme.ru/embed/tv?tmdb=$tmdbId&season=$season&episode=$episode"

        "2embed" -> if (isMovie) {
            if (hasImdb) "https://www.2embed.cc/embed/$imdbId"
            else "https://www.2embed.cc/embed/$tmdbId"
        } else {
            if (hasImdb) "https://www.2embed.cc/embedtv/$imdbId&s=$season&e=$episode"
            else "https://www.2embed.cc/embedtv/$tmdbId&s=$season&e=$episode"
        }

        "autoembed" -> if (isMovie)
            "https://player.autoembed.cc/embed/movie/$tmdbId"
        else
            "https://player.autoembed.cc/embed/tv/$tmdbId/$season/$episode"

        "superembed" -> if (isMovie)
            "https://multiembed.mov/?video_id=$tmdbId&tmdb=1"
        else
            "https://multiembed.mov/?video_id=$tmdbId&tmdb=1&s=$season&e=$episode"

        else -> "about:blank"
    }
}

// ─── Composable player screen ─────────────────────────────────────────────────

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
    var errorMsg       by remember { mutableStateOf<String?>(null) }

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

    // Reset loading on URL change
    LaunchedEffect(embedUrl) {
        isLoading = true
        errorMsg = null
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
        // ── WebView ─────────────────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        mediaPlaybackRequiresUserGesture = false
                        userAgentString =
                            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        allowFileAccess = false
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                    }
                    webChromeClient = object : WebChromeClient() {}
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            isLoading = false
                        }
                        override fun onReceivedError(
                            view: WebView,
                            errorCode: Int,
                            description: String,
                            failingUrl: String
                        ) {
                            isLoading = false
                            // Only show error for the main frame
                            if (failingUrl == embedUrl) {
                                errorMsg = "Server unreachable. Try another server."
                            }
                        }
                        @Deprecated("Deprecated in API 24")
                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                            // Allow all navigation inside WebView
                            return false
                        }
                    }
                    loadUrl(embedUrl)
                }
            },
            update = { wv ->
                val currentUrl = wv.url ?: ""
                if (currentUrl != embedUrl && !currentUrl.contains(embedUrl.take(30))) {
                    isLoading = true
                    errorMsg = null
                    wv.loadUrl(embedUrl)
                }
            }
        )

        // Loading spinner
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFE50914),
                strokeWidth = 3.dp
            )
        }

        // Error message
        if (errorMsg != null && !isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(errorMsg!!, color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        errorMsg = null
                        isLoading = true
                        webViewRef?.loadUrl(embedUrl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) { Text("Retry") }
            }
        }

        // ── Top controls overlay ─────────────────────────────────────────────
        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.70f))
            ) {
                // Title row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = if (mediaType == "movie") title
                               else "$title  S${season}E${episode}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        isLoading = true
                        errorMsg = null
                        webViewRef?.reload()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
                    }
                }

                // Server tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                    errorMsg = null
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) Color(0xFFE50914)
                                    else Color.White.copy(alpha = 0.18f)
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

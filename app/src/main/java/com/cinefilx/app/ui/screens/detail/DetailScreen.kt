package com.cinefilx.app.ui.screens.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cinefilx.app.data.model.EztvTorrent
import com.cinefilx.app.data.model.MediaType
import com.cinefilx.app.data.model.TmdbCast
import com.cinefilx.app.data.model.TmdbEpisode
import com.cinefilx.app.data.model.TmdbMovieDetail
import com.cinefilx.app.data.model.TmdbSeason
import com.cinefilx.app.data.model.YtsTorrent
import com.cinefilx.app.ui.screens.player.PlayerActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    mediaId: Int,
    mediaType: MediaType,
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState         by viewModel.uiState.collectAsStateWithLifecycle()
    val isInWatchlist   by viewModel.isInWatchlist.collectAsStateWithLifecycle()
    val context         = LocalContext.current
    var showTorrentSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingBox()
            uiState.error != null -> ErrorBox(uiState.error!!) { viewModel.loadDetail() }
            uiState.detail != null -> {
                DetailContent(
                    detail        = uiState.detail!!,
                    mediaType     = uiState.mediaType,
                    imdbId        = uiState.imdbId,
                    isInWatchlist = isInWatchlist,
                    selectedSeason = uiState.selectedSeason,
                    seasonDetail  = uiState.seasonDetail,
                    seasonLoading = uiState.seasonLoading,
                    onPlayClick   = { season, episode ->
                        launchPlayer(
                            context   = context,
                            tmdbId    = mediaId,
                            imdbId    = uiState.imdbId ?: "",
                            mediaType = uiState.mediaType.value,
                            season    = season,
                            episode   = episode,
                            title     = uiState.detail!!.displayTitle
                        )
                    },
                    onWatchlistClick  = { viewModel.toggleWatchlist() },
                    onTorrentClick    = {
                        viewModel.loadTorrents()
                        showTorrentSheet = true
                    },
                    onSeasonSelect    = { viewModel.loadSeason(it) }
                )
            }
        }

        // Back button (always visible)
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }

    // ── Torrent bottom sheet ───────────────────────────────────────────────────
    if (showTorrentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTorrentSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            TorrentSheet(
                mediaType     = uiState.mediaType,
                movieTorrents = uiState.movieTorrents,
                tvTorrents    = uiState.tvTorrents,
                isLoading     = uiState.torrentLoading,
                error         = uiState.torrentError,
                onMagnetClick = { magnet -> openMagnet(context, magnet) },
                onCopyClick   = { magnet -> copyToClipboard(context, magnet) }
            )
        }
    }
}

// ── helpers ───────────────────────────────────────────────────────────────────

private fun launchPlayer(
    context: Context,
    tmdbId: Int,
    imdbId: String,
    mediaType: String,
    season: Int,
    episode: Int,
    title: String
) {
    context.startActivity(
        Intent(context, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_TMDB_ID,    tmdbId)
            putExtra(PlayerActivity.EXTRA_IMDB_ID,    imdbId)
            putExtra(PlayerActivity.EXTRA_MEDIA_TYPE, mediaType)
            putExtra(PlayerActivity.EXTRA_SEASON,     season)
            putExtra(PlayerActivity.EXTRA_EPISODE,    episode)
            putExtra(PlayerActivity.EXTRA_TITLE,      title)
        }
    )
}

private fun openMagnet(context: Context, magnet: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(magnet)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open with torrent app")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        // No torrent app installed — copy magnet to clipboard
        copyToClipboard(context, magnet)
        Toast.makeText(
            context,
            "No torrent app found. Magnet link copied — paste in LibreTorrent/1DM.",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Magnet Link", text))
    Toast.makeText(context, "Magnet link copied", Toast.LENGTH_SHORT).show()
}

// ── main content ──────────────────────────────────────────────────────────────

@Composable
private fun DetailContent(
    detail: TmdbMovieDetail,
    mediaType: MediaType,
    imdbId: String?,
    isInWatchlist: Boolean,
    selectedSeason: Int,
    seasonDetail: com.cinefilx.app.data.model.TmdbSeasonDetail?,
    seasonLoading: Boolean,
    onPlayClick: (season: Int, episode: Int) -> Unit,
    onWatchlistClick: () -> Unit,
    onTorrentClick: () -> Unit,
    onSeasonSelect: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ── Hero backdrop ──────────────────────────────────────────────────
        item {
            Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
                AsyncImage(
                    model = detail.backdropUrl().ifEmpty { detail.posterUrl() },
                    contentDescription = detail.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surface
                            ),
                            startY = 100f
                        )
                    )
                )
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Card(modifier = Modifier.size(100.dp, 150.dp), shape = MaterialTheme.shapes.medium) {
                        AsyncImage(
                            model = detail.posterUrl(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = detail.displayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                            Text(String.format("%.1f", detail.voteAverage), style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFC107))
                            Text("• ${detail.year}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (detail.displayRuntime.isNotEmpty()) {
                                Text("• ${detail.displayRuntime}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            detail.genres.take(3).forEach { genre ->
                                Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(genre.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Action buttons ────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onPlayClick(
                            if (mediaType == MediaType.MOVIE) 1 else selectedSeason,
                            1
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Watch", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onTorrentClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Download, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Torrent")
                }
                IconButton(onClick = onWatchlistClick) {
                    Icon(
                        if (isInWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ── Overview ──────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(detail.overview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ── Tagline ───────────────────────────────────────────────────────
        if (!detail.tagline.isNullOrEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        "\"${detail.tagline}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // ── Seasons / Episodes (TV only) ──────────────────────────────────
        if (mediaType != MediaType.MOVIE) {
            val seasons = detail.seasons?.filter { it.seasonNumber > 0 } ?: emptyList()
            if (seasons.isNotEmpty()) {
                item {
                    SeasonsSection(
                        seasons        = seasons,
                        selectedSeason = selectedSeason,
                        onSeasonSelect = onSeasonSelect
                    )
                }
            }

            if (seasonLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else if (seasonDetail != null) {
                items(seasonDetail.episodes) { episode ->
                    EpisodeRow(
                        episode = episode,
                        onClick = { onPlayClick(selectedSeason, episode.episodeNumber) }
                    )
                }
            }
        }

        // ── Cast ──────────────────────────────────────────────────────────
        val cast = detail.credits?.cast?.take(12) ?: emptyList()
        if (cast.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Cast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cast) { CastCard(it) }
                    }
                }
            }
        }

        // ── Details card ──────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (detail.status.isNotEmpty()) InfoRow("Status", detail.status)
                    if (!imdbId.isNullOrEmpty()) InfoRow("IMDB", imdbId)
                    if (detail.numberOfSeasons != null) InfoRow("Seasons", "${detail.numberOfSeasons}")
                    if (detail.numberOfEpisodes != null) InfoRow("Episodes", "${detail.numberOfEpisodes}")
                    val languages = detail.spokenLanguages.take(3).joinToString(", ") { it.englishName }
                    if (languages.isNotEmpty()) InfoRow("Languages", languages)
                }
            }
        }
    }
}

// ── Seasons section ───────────────────────────────────────────────────────────

@Composable
private fun SeasonsSection(
    seasons: List<TmdbSeason>,
    selectedSeason: Int,
    onSeasonSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            "Seasons",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(seasons) { season ->
                val selected = season.seasonNumber == selectedSeason
                FilterChip(
                    selected = selected,
                    onClick  = { onSeasonSelect(season.seasonNumber) },
                    label    = { Text("S${season.seasonNumber}") }
                )
            }
        }
    }
}

// ── Episode row ───────────────────────────────────────────────────────────────

@Composable
private fun EpisodeRow(episode: TmdbEpisode, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail or placeholder
            if (episode.stillPath != null) {
                AsyncImage(
                    model = episode.stillUrl(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(100.dp, 60.dp).clip(MaterialTheme.shapes.small)
                )
            } else {
                Box(
                    Modifier
                        .size(100.dp, 60.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayCircle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "E${episode.episodeNumber} • ${episode.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (episode.overview.isNotEmpty()) {
                    Text(
                        episode.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                if (episode.runtime != null) {
                    Text("${episode.runtime}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
    }
}

// ── Torrent sheet ─────────────────────────────────────────────────────────────

@Composable
private fun TorrentSheet(
    mediaType: MediaType,
    movieTorrents: List<YtsTorrent>,
    tvTorrents: List<EztvTorrent>,
    isLoading: Boolean,
    error: String?,
    onMagnetClick: (String) -> Unit,
    onCopyClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            "Torrents",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )

        when {
            isLoading -> Box(
                Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            error != null -> Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Text(
                    "Could not load torrents:\n$error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tip: Install LibreTorrent, 1DM, or BitTorrent app to open magnet links.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            mediaType == MediaType.MOVIE -> {
                if (movieTorrents.isEmpty()) {
                    Text(
                        "No torrents found for this movie.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(movieTorrents) { t ->
                            TorrentRow(
                                title    = "${t.quality} • ${t.type.ifEmpty { "web" }}",
                                info     = "${t.size} • ${t.seeds} seeds",
                                magnet   = t.magnetUrl,
                                onMagnet = onMagnetClick,
                                onCopy   = onCopyClick
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }

            else -> {
                if (tvTorrents.isEmpty()) {
                    Text(
                        "No torrents found for this show.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(tvTorrents.take(50)) { t ->
                            TorrentRow(
                                title    = t.title.let { if (it.length > 70) it.take(67) + "…" else it },
                                info     = "${t.displaySize} • ${t.seeds} seeds",
                                magnet   = t.magnetUrl,
                                onMagnet = onMagnetClick,
                                onCopy   = onCopyClick
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TorrentRow(
    title: String,
    info: String,
    magnet: String,
    onMagnet: (String) -> Unit,
    onCopy: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMagnet(magnet) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(info, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { onMagnet(magnet) }) {
            Icon(Icons.Filled.Download, "Open magnet", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = { onCopy(magnet) }) {
            Icon(Icons.Filled.ContentCopy, "Copy magnet", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CastCard(castMember: TmdbCast) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Card(modifier = Modifier.size(60.dp), shape = CircleShape) {
            if (castMember.profilePath != null) {
                AsyncImage(model = castMember.profileUrl(), contentDescription = castMember.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(castMember.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorBox(msg: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text(msg, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

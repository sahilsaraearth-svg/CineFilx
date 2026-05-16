# CineFilx - Full Rebuild Plan

## Architecture
- TMDB API → metadata, posters, search, trending
- TMDB external_ids → get imdb_id per content
- Streaming: WebView embeds (primary) + ExoPlayer (HLS/MP4 if extractable)
- Torrents: EZTV (TV) + YTS (movies) → magnet links → download manager

## Streaming Sources (priority order in player)
1. VidSrc.to  — https://vidsrc.to/embed/movie/{imdb_id} | /tv/{imdb_id}/{s}/{e}
2. VidSrc.me  — https://vidsrc.me/embed/movie?tmdb={id} | /tv?tmdb={id}&season={s}&episode={e}
3. 2embed     — https://www.2embed.cc/embed/{imdb_id} | /embedtv/{imdb_id}&s={s}&e={e}
4. AutoEmbed  — https://autoembed.co/movie/tmdb/{id} | /tv/tmdb/{id}-{s}-{e}
5. SuperEmbed — https://multiembed.mov/?video_id={id}&tmdb=1 | &s={s}&e={e}

## Torrent Sources
- YTS: https://yts.mx/api/v2/movie_details.json?imdb_id={imdb_id} → magnet
- EZTV: https://eztv.re/api/get-torrents?imdb_id={bare_id} → magnet per episode

## Files to rewrite/create
1. [x] Media.kt - add imdbId field
2. [ ] StreamingSource.kt - enum + URL builder
3. [ ] TorrentRepository.kt - YTS + EZTV API calls
4. [ ] PlayerActivity.kt - WebView player + source switcher + ExoPlayer fallback
5. [ ] DetailScreen.kt - play button → source selector bottom sheet → launch player
6. [ ] WatchlistDao.kt + WatchlistDatabase.kt - Room DB for favorites
7. [ ] DownloadManager.kt - handle magnet/torrent downloads
8. [ ] TvDetailScreen.kt - seasons/episodes list
9. [ ] MediaRepository.kt - add getExternalIds(), getTvSeasons()
10. [ ] MainActivity.kt - add Watchlist tab
11. [ ] AndroidManifest.xml - add permissions (WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)
12. [ ] build.gradle.kts - add Room deps

## Key decisions
- WebView with JavaScript enabled, custom user-agent = Android Chrome
- Source switcher: bottom sheet with 5 server buttons
- Torrent = intent to open magnet in external torrent app OR in-app via download manager
- Watchlist = Room SQLite, persist forever
- IMDB ID cached after first fetch (stored in MediaItem)

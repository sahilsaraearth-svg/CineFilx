package com.cinefilx.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: Int,          // TMDB ID
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val mediaType: String,            // "movie" / "tv" / "anime"
    val imdbId: String?,
    val year: String,
    val rating: Double,
    val addedAt: Long = System.currentTimeMillis()
)

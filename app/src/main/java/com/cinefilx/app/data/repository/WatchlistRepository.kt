package com.cinefilx.app.data.repository

import com.cinefilx.app.data.local.WatchlistDao
import com.cinefilx.app.data.local.WatchlistItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepository @Inject constructor(
    private val dao: WatchlistDao
) {
    fun getAllItems(): Flow<List<WatchlistItem>> = dao.getAllItems()

    fun isInWatchlist(id: Int): Flow<Boolean> = dao.existsFlow(id)

    suspend fun addToWatchlist(item: WatchlistItem) = dao.insert(item)

    suspend fun removeFromWatchlist(id: Int) = dao.deleteById(id)

    suspend fun toggleWatchlist(item: WatchlistItem): Boolean {
        return if (dao.exists(item.id)) {
            dao.deleteById(item.id)
            false
        } else {
            dao.insert(item)
            true
        }
    }
}

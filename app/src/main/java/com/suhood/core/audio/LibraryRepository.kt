package com.suhood.core.audio

import com.suhood.core.database.FavoriteTrackEntity
import com.suhood.core.database.RecentTrackEntity
import com.suhood.core.database.SuhoodDao
import kotlinx.coroutines.flow.Flow

class LibraryRepository(private val dao: SuhoodDao) {
    fun observeFavorites(): Flow<List<String>> = dao.observeFavorites()
    fun observeRecent(): Flow<List<String>> = dao.observeRecent()

    suspend fun toggleFavorite(trackId: String) {
        if (dao.isFavorite(trackId)) dao.removeFavorite(trackId)
        else dao.addFavorite(FavoriteTrackEntity(trackId))
    }

    suspend fun addRecent(trackId: String) {
        dao.addRecent(RecentTrackEntity(trackId))
    }
}

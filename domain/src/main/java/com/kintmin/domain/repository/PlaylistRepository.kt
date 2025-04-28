package com.kintmin.domain.repository

import com.kintmin.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    suspend fun addPlaylist(newValue: Playlist): Result<Unit>
    fun getPlaylistListFlow(): Flow<List<Playlist>>
    fun getPlaylistFlow(playlistId: Int): Flow<Playlist>
    suspend fun updatePlaylist(id: Long, updateValue: Playlist): Result<Unit>
    suspend fun deletePlaylist(id: Long): Result<Unit>
}
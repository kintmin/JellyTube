package com.kintmin.domain.repository

import com.kintmin.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    suspend fun addPlaylist(title: String): Result<Unit>
    fun getPlaylistListFlow(): Flow<List<Playlist>>
    fun getPlaylistFlow(playlistId: Int): Flow<Playlist>
    suspend fun getPlaylistById(playlistId: Int): Result<Playlist>
    suspend fun updatePlaylistName(id: Int, newName: String): Result<Unit>
    suspend fun updatePlaylistDescription(id: Int, newDescription: String): Result<Unit>
    suspend fun updatePlaylistPlayback(id: Int, mediaCount: Int, totalDuration: Long): Result<Unit>
    suspend fun updatePlaylistImage(id: Int, imageFileNameWithExt: String?): Result<Unit>
    suspend fun deletePlaylist(id: Int): Result<Unit>
}
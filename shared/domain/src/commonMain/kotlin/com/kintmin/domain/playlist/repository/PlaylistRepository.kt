package com.kintmin.domain.playlist.repository

import com.kintmin.domain.playlist.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    suspend fun addPlaylist(title: String): Result<Int>

    fun getAllPlaylistFlow(): Flow<List<Playlist>>
    fun getPlaylistFlow(playlistId: Int): Flow<Playlist>

    suspend fun updatePlaylist(
        id: Int,
        name: String? = null,
        description: String? = null,
        imageFileFullPath: String? = null,
        audioMediaCount: Int? = null,
        rawTotalDuration: Long? = null,
    ): Result<Unit>

    suspend fun deletePlaylist(id: Int): Result<Unit>
}
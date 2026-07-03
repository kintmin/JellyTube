package com.kintmin.domain.playlist.repository

import com.kintmin.domain.playlist.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    // 시스템 플레이리스트(전체/미분류/즐겨찾기)가 없으면 만든다. 멱등.
    suspend fun ensureSystemPlaylists(): Result<Unit>

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

    suspend fun updatePlaylistSequences(orderedPlaylistIds: List<Int>): Result<Unit>
}
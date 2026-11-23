package com.kintmin.domain.playlist.repository

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    // 시스템 플레이리스트(전체/미분류/즐겨찾기)가 없으면 만든다. 멱등.
    suspend fun ensureSystemPlaylists(): Result<Unit>

    suspend fun addPlaylist(title: String, type: PlaylistType): Result<Int>

    suspend fun getPlaylist(playlistId: Int): Result<Playlist>

    fun getAllPlaylistFlow(): Flow<List<Playlist>>
    // 없는 플레이리스트에서는 아무것도 내보내지 않는다. 한 번만 읽으려면 getPlaylist 를 쓴다.
    fun getPlaylistFlow(playlistId: Int): Flow<Playlist>

    suspend fun updatePlaylist(
        id: Int,
        name: String? = null,
        description: String? = null,
        imageFileFullPath: String? = null,
        audioMediaCount: Int? = null,
        rawTotalDuration: Long? = null,
    ): Result<Unit>

    suspend fun setCustomPlaylistImage(id: Int, imageData: ByteArray): Result<Unit>

    suspend fun resetPlaylistImage(id: Int): Result<Unit>

    suspend fun deletePlaylist(id: Int): Result<Unit>

    suspend fun updatePlaylistSequences(orderedPlaylistIds: List<Int>): Result<Unit>
}
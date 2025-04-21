package com.kintmin.domain.repository

import com.kintmin.domain.model.Playlist

interface PlaylistRepository {
    suspend fun addPlaylist(newValue: Playlist): Result<Unit>
    suspend fun getPlaylist(): Result<List<Playlist>>
    suspend fun updatePlaylist(id: Long, updateValue: Playlist): Result<Unit>
    suspend fun deletePlaylist(id: Long): Result<Unit>
}
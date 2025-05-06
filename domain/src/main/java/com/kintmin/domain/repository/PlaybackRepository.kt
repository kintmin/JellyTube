package com.kintmin.domain.repository

import kotlinx.coroutines.flow.Flow

interface PlaybackRepository {
    suspend fun addAudioMediaListToPlaylist(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit>
    fun getIsPlaybackRepeatingFlow(): Flow<Boolean>
    fun getIsPlaybackShufflingFlow(): Flow<Boolean>
    suspend fun getAudioMediaIdList(playlistId: Int): Result<List<Int>>
    suspend fun getPlaylistIdList(audioMediaId: Int): Result<List<Int>>
    suspend fun setIsPlaybackShuffling(isShuffling: Boolean): Result<Unit>
    suspend fun setPlaybackRepeating(isRepeating: Boolean): Result<Unit>
    suspend fun updatePlaybackSequence(playlistId: Int, audioMediaId: Int, newSequence: Int): Result<Unit>
    suspend fun deletePlaylistTrack(playlistId: Int, audioMediaId: Int): Result<Unit>
}
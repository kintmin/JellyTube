package com.kintmin.domain.audio_track.repository

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import kotlinx.coroutines.flow.Flow

interface AudioTrackRepository {
    suspend fun addAudioTrack(playlistId: Int, audioMediaId: Int): Result<Unit>
    suspend fun addAudioTrackList(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit>

    fun getPlaylistTrackAggregateFlow(playlistId: Int, audioMediaId: Int): Flow<PlaylistTrackAggregate>
    fun getPlaylistTrackAggregateListFlow(playlistId: Int): Flow<List<PlaylistTrackAggregate>>

    suspend fun getPlaylistIdList(audioMediaId: Int): Result<List<Int>>

    suspend fun updateTrackSequence(playlistId: Int, audioMediaId: Int, newSequence: Int): Result<Unit>
    suspend fun deleteAudioTrackList(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit>
}
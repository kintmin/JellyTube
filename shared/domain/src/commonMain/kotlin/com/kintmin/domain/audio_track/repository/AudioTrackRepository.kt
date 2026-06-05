package com.kintmin.domain.audio_track.repository

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import kotlinx.coroutines.flow.Flow

interface AudioTrackRepository {

    suspend fun addCustomAudioTrack(playlistId: Int, audioMediaIdList: List<Int>): Result<Int>

    suspend fun deleteCustomAudioTrack(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit>

    fun getPlaylistTrackAggregateFlow(playlistId: Int, audioMediaId: Int): Flow<PlaylistTrackAggregate>
    fun getPlaylistTrackAggregateListFlow(playlistId: Int): Flow<List<PlaylistTrackAggregate>>

    fun getPlaylistIdListFlow(audioMediaId: Int): Flow<List<Int>>

    suspend fun getPlaylistTrackCount(playlistId: Int): Result<Int>

    suspend fun updateTrackSequence(playlistId: Int, audioMediaId: Int, oldSequence: Int, newSequence: Int): Result<Unit>
}
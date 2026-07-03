package com.kintmin.domain.audio_track.repository

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import kotlinx.coroutines.flow.Flow

interface AudioTrackRepository {

    suspend fun addCustomAudioTrack(playlistId: Int, audioMediaIdList: List<Int>): Result<Int>

    suspend fun deleteCustomAudioTrack(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit>

    fun getPlaylistTrackAggregateFlow(playlistId: Int, audioMediaId: Int): Flow<PlaylistTrackAggregate>
    fun getPlaylistTrackAggregateListFlow(playlistId: Int): Flow<List<PlaylistTrackAggregate>>

    fun getPlaylistIdListFlow(audioMediaId: Int): Flow<List<Int>>

    // 즐겨찾기 on/off. 미분류 동기화에 영향을 주지 않는 전용 경로.
    suspend fun setFavorite(audioMediaId: Int, isFavorite: Boolean): Result<Unit>

    suspend fun getPlaylistTrackCount(playlistId: Int): Result<Int>

    suspend fun updateTrackSequence(playlistId: Int, audioMediaId: Int, oldSequence: Int, newSequence: Int): Result<Unit>
}
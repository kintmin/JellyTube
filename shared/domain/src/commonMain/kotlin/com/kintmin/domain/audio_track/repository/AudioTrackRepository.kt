package com.kintmin.domain.audio_track.repository

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import kotlinx.coroutines.flow.Flow

interface AudioTrackRepository {

    suspend fun addCustomAudioTrack(playlistId: Int, audioMediaIdList: List<Int>): Result<Int>

    suspend fun deleteCustomAudioTrack(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit>

    fun getPlaylistTrackAggregateFlow(playlistId: Int, audioMediaId: Int): Flow<PlaylistTrackAggregate>
    /** [showSecretInTotal] 가 false 면 "전체" 에서 비밀 플레이리스트에 속한 곡을 뺀다. */
    fun getPlaylistTrackAggregateListFlow(playlistId: Int, showSecretInTotal: Boolean = false): Flow<List<PlaylistTrackAggregate>>

    fun getPlaylistIdListFlow(audioMediaId: Int): Flow<List<Int>>

    fun getSecretAudioMediaIdListFlow(): Flow<List<Int>>

    // 즐겨찾기 on/off. 미분류 동기화에 영향을 주지 않는 전용 경로.
    suspend fun setFavorite(audioMediaId: Int, isFavorite: Boolean): Result<Unit>

    suspend fun getPlaylistTrackCount(playlistId: Int): Result<Int>

    suspend fun updateTrackSequence(playlistId: Int, audioMediaId: Int, oldSequence: Int, newSequence: Int): Result<Unit>

    /** 재생목록의 곡을 [orderedAudioMediaIds] 순서대로 한 번에 다시 매긴다. 재생목록의 곡을 모두 담아야 한다. */
    suspend fun reorderTracks(playlistId: Int, orderedAudioMediaIds: List<Int>): Result<Unit>
}
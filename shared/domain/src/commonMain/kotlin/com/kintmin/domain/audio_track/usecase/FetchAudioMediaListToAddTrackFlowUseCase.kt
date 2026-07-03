package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

class FetchAudioMediaListToAddTrackFlowUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val fetchPlaylistsByTypeUseCase: FetchPlaylistsByTypeUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(playlistId: Int): Flow<List<PlaylistTrackAggregate>> {
        return fetchPlaylistsByTypeUseCase(PlaylistType.TOTAL)
            .flatMapLatest { totalPlaylists ->
                val totalId = totalPlaylists.firstOrNull()?.id ?: return@flatMapLatest flowOf(emptyList())
                combine(
                    audioTrackRepository.getPlaylistTrackAggregateListFlow(totalId),
                    audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId),
                ) { totalList, currentList ->
                    // 전체 중 현재 playlist에 포함되지 않는 것만 필터
                    totalList.filter { total -> currentList.firstOrNull { it.audioMedia.id == total.audioMedia.id } == null }
                }
            }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
    }
}

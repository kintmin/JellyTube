package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

class FetchAudioMediaDetailFlowUseCase constructor(
    private val playlistTrackRepository: AudioTrackRepository,
    private val fetchSecretModeEnabledFlowUseCase: FetchSecretModeEnabledFlowUseCase,
) {
    operator fun invoke(audioMediaId: Int): Flow<List<PlaylistTrackAggregate>> {
        // 모든 음원이 "전체" 에 들어 있어 비밀 플레이리스트를 빼도 목록이 비지 않는다.
        return combine(linkedAggregatesFlow(audioMediaId), fetchSecretModeEnabledFlowUseCase()) { aggregates, isSecretModeEnabled ->
            aggregates.filter { it.playlist.type.isVisible(isSecretModeEnabled) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun linkedAggregatesFlow(audioMediaId: Int): Flow<List<PlaylistTrackAggregate>> {
        return playlistTrackRepository.getPlaylistIdListFlow(audioMediaId)
            .flowOn(Dispatchers.IO)
            .flatMapLatest { playlistIds ->
                if (playlistIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        playlistIds.map { playlistId ->
                            playlistTrackRepository
                                .getPlaylistTrackAggregateFlow(playlistId, audioMediaId)
                                .flowOn(Dispatchers.IO)
                        }
                    ) { it.toList() }
                }
            }
    }
}

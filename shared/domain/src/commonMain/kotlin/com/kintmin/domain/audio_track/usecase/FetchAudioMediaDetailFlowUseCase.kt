package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
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
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(audioMediaId: Int): Flow<List<PlaylistTrackAggregate>> {
        // 해당 오디오의 플레이리스트를 먼저 가져온다
        return playlistTrackRepository.getPlaylistIdListFlow(audioMediaId)
            .flowOn(Dispatchers.IO)
            // 해당 플레이리스트가 변경되면 flow 재시작
            .flatMapLatest { playlistIds ->
                if (playlistIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    // 플레이리스트 flow 가 모두 이어져 있고,
                    // 하나라도 변경될 경우 재시 collect 해야하기에 combine 사용
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

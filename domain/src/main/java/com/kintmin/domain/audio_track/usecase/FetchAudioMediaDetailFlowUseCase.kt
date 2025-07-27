package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class FetchAudioMediaDetailFlowUseCase @Inject constructor(
    private val playlistTrackRepository: AudioTrackRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(audioMediaId: Int): Flow<List<PlaylistTrackAggregate>> {
        // 연결된 플레이리스트를 전부 가져온다.
        return playlistTrackRepository.getPlaylistIdListFlow(audioMediaId)
            .flowOn(Dispatchers.IO)
            // 연결된 플레이리스트가 변경 시 flow 다시 생성
            .flatMapLatest { playlistIds ->
                if (playlistIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    // 플레이리스트 flow 가 전부 적어도 1번 collect 되어야 하고,
                    // 하나라도 변경될 시 다시 collect 해야되기에 combine 사용
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
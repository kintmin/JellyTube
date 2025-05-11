package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FetchAudioMediaDetailFlowUseCase @Inject constructor(
    private val playlistTrackRepository: AudioTrackRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(audioMediaId: Int): Flow<List<PlaylistTrackAggregate>> {
        return playlistTrackRepository.getPlaylistIdListFlow(audioMediaId)
            .flatMapLatest { playlistIds ->
                if (playlistIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        playlistIds.map { playlistId ->
                            playlistTrackRepository.getPlaylistTrackAggregateFlow(playlistId, audioMediaId)
                        }
                    ) { it.toList() }
                }
            }
    }
}
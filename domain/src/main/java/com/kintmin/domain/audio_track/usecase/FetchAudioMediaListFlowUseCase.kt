package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchAudioMediaListFlowUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    operator fun invoke(playlistId: Int): Flow<List<PlaylistTrackAggregate>> {
        return audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId)
    }
}
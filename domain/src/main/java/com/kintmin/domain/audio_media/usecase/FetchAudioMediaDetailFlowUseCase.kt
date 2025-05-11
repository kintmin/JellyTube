package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchAudioMediaDetailFlowUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    operator fun invoke(playlistId: Int, audioMediaId: Int): Flow<PlaylistTrackAggregate> {
        return audioTrackRepository.getPlaylistTrackAggregateFlow(playlistId, audioMediaId)
    }
}
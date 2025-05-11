package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchPlaylistTrackAggregateFlowUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    operator fun invoke(playlistId: Int, audioMediaId: Int): Flow<PlaylistTrackAggregate> {
        return audioTrackRepository.getPlaylistTrackAggregateFlow(playlistId, audioMediaId)
    }
}
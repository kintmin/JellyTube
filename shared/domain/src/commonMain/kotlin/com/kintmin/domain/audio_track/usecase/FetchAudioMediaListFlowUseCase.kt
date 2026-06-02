package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class FetchAudioMediaListFlowUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    operator fun invoke(playlistId: Int): Flow<List<PlaylistTrackAggregate>> {
        return audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId).flowOn(Dispatchers.IO)
    }
}
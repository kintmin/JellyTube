package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.PlaylistType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class FetchAudioMediaListFlowUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    operator fun invoke(playlistId: Int): Flow<List<PlaylistTrackAggregate>> {
        return combine(
            audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId),
            audioTrackRepository.getSecretTrackListFlow(),
        ) { tracks, secretTracks ->
            if (tracks.firstOrNull()?.playlist?.type != PlaylistType.TOTAL) return@combine tracks
            val secretIds = secretTracks.mapTo(HashSet()) { it.audioMediaId }
            tracks.filterNot { it.audioMedia.id in secretIds }
        }.flowOn(Dispatchers.IO)
    }
}
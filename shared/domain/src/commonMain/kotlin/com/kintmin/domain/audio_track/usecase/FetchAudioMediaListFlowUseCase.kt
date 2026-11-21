package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class FetchAudioMediaListFlowUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val playlistRepository: PlaylistRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(playlistId: Int): Flow<List<PlaylistTrackAggregate>> {
        val tracksFlow = audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId)
        return playlistRepository.getPlaylistFlow(playlistId)
            .map { it.type == PlaylistType.TOTAL }
            .distinctUntilChanged()
            .flatMapLatest { isTotal ->
                if (!isTotal) return@flatMapLatest tracksFlow
                combine(tracksFlow, audioTrackRepository.getSecretTrackListFlow()) { tracks, secretTracks ->
                    val secretIds = secretTracks.mapTo(HashSet()) { it.audioMediaId }
                    tracks.filterNot { it.audioMedia.id in secretIds }
                }
            }
            .flowOn(Dispatchers.IO)
    }
}

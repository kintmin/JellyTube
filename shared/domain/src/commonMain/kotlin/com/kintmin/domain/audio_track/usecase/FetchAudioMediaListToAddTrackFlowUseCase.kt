package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

class FetchAudioMediaListToAddTrackFlowUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    operator fun invoke(playlistId: Int): Flow<List<PlaylistTrackAggregate>> {
        return combine(
            audioTrackRepository.getPlaylistTrackAggregateListFlow(Playlist.TOTAL),
            audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId),
        ) { totalList, currentList ->
            // ?„ì²´ ì¤??„ìž¬ playlist???¬í•¨?˜ì? ?ŠëŠ” ê²ƒë§Œ ?„í„°
            totalList.filter { total -> currentList.firstOrNull { it.audioMedia.id == total.audioMedia.id } == null }
        }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
    }
}
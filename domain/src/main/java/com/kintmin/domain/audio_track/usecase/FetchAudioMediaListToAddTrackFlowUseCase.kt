package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class FetchAudioMediaListToAddTrackFlowUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
){
    operator fun invoke(playlistId: Int): Flow<List<PlaylistTrackAggregate>> {
        return combine(
            audioTrackRepository.getPlaylistTrackAggregateListFlow(Playlist.TOTAL),
            audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId),
        ) { totalList, currentList ->
            totalList.filter { total -> currentList.firstOrNull { it.audioMedia.id == total.audioMedia.id } == null }
        }.distinctUntilChanged()
    }
}
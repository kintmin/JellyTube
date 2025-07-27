package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val addUncategorizedPlaylistUseCase: AddUncategorizedPlaylistUseCase,
) {

    suspend operator fun invoke(playlistId: Int) {
        runCatching {
            val audioMediaIdList = audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId)
                .flowOn(Dispatchers.IO)
                .map { aggregateList ->
                    aggregateList.map { it.audioMedia.id }
                }.first()
            playlistRepository.deletePlaylist(playlistId).getOrThrow()
            addUncategorizedPlaylistUseCase(audioMediaIdList)
        }
    }
}
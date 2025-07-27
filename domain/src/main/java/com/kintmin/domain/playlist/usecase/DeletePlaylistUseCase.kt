package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val addUncategorizedPlaylistUseCase: AddUncategorizedPlaylistUseCase,
    private val log: Log,
) {

    suspend operator fun invoke(playlistId: Int) {
        runCatching {
            val trackList = audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId)
                .flowOn(Dispatchers.IO)
                .first()

            playlistRepository.deletePlaylist(playlistId).onSuccess {
                val playlist = trackList.firstOrNull()?.playlist ?: return@onSuccess
                log.sendFirebaseEvent(
                    FirebaseEvent.DeletePlaylist(
                        playlistId = playlist.id,
                        playlistTitle = playlist.name,
                        audioMediaCount = trackList.count(),
                    )
                )
            }.getOrThrow()

            val audioMediaIdList = trackList.map { it.audioMedia.id }
            addUncategorizedPlaylistUseCase(audioMediaIdList)
        }
    }
}
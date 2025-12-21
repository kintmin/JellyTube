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
    private val log: Log,
) {

    suspend operator fun invoke(playlistId: Int) {
        runCatching {
            val playlistToDelete = playlistRepository.getPlaylistFlow(playlistId)
                .flowOn(Dispatchers.IO)
                .first()

            val trackCount = audioTrackRepository.getPlaylistTrackCount(playlistId).getOrThrow()

            playlistRepository.deletePlaylist(playlistId).onSuccess {
                log.sendFirebaseEvent(
                    FirebaseEvent.DeletePlaylist(
                        playlistId = playlistToDelete.id,
                        playlistTitle = playlistToDelete.name,
                        audioMediaCount = trackCount,
                    )
                )
            }.getOrThrow()
        }
    }
}
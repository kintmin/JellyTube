package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.FirebaseEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn

class DeletePlaylistUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(playlistId: Int) {
        runCatching {
            val playlistToDelete = playlistRepository.getPlaylistFlow(playlistId)
                .flowOn(Dispatchers.IO)
                .first()

            val trackCount = audioTrackRepository.getPlaylistTrackCount(playlistId).getOrThrow()

            playlistRepository.deletePlaylist(playlistId).onSuccess {
                appLog.sendFirebaseEvent(
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

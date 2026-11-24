package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.SentryEvent
import com.kintmin.log.secret

class DeletePlaylistUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val playlistRepository: PlaylistRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(playlistId: Int): Result<Unit> {
        return suspendRunCatching {
            val playlistToDelete = playlistRepository.getPlaylist(playlistId).getOrThrow()

            val trackCount = audioTrackRepository.getPlaylistTrackCount(playlistId).getOrThrow()

            playlistRepository.deletePlaylist(playlistId).onSuccess {
                appLog.sendSentryEvent(
                    SentryEvent.DeletePlaylist(
                        playlistId = playlistToDelete.id,
                        playlistTitle = secret(playlistToDelete.name, playlistToDelete.type.isSecret),
                        audioMediaCount = trackCount,
                    )
                )
            }.getOrThrow()
        }
    }
}

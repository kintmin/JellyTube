package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdatePlaylistImageWhenUpdateTrackUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(playlistId: Int) {
        val playlist = playlistRepository.getPlaylistFlow(playlistId).first()
        if (playlist.isCustomImage) return

        val firstData = audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId).first().minByOrNull {
            it.audioTrack.trackSequence
        }

        if (firstData != null) {
            firstData.audioMedia.imageFileFullPath?.let {
                if (it == playlist.imageFileFullPath) return
                playlistRepository.updatePlaylist(
                    id = playlistId,
                    imageFileFullPath = it,
                )
            }
        }
    }
}
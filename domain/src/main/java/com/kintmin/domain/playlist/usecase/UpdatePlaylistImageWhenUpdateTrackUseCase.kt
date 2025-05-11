package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdatePlaylistImageWhenUpdateTrackUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(playlistId: Int) {
        withContext(Dispatchers.IO) {
            val playlist = playlistRepository.getPlaylistFlow(playlistId).first()
            if (playlist.isCustomImage) return@withContext

            val firstData = audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId).first().minByOrNull {
                it.audioTrack.trackSequence
            }

            if (firstData != null) {
                firstData.audioMedia.imageFileFullPath?.let {
                    if (it == playlist.imageFileFullPath) return@withContext
                    playlistRepository.updatePlaylist(
                        id = playlistId,
                        imageFileFullPath = it,
                    )
                }
            }
        }
    }
}
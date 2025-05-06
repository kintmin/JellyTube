package com.kintmin.domain.internal_usecase

import com.kintmin.domain.repository.AudioMediaRepository
import com.kintmin.domain.repository.PlaylistRepository
import javax.inject.Inject

class UpdatePlaylistImageWhenUpdatePlaybackUseCase  @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(playlistId: Int) {
        val playlist = playlistRepository.getPlaylistById(playlistId).getOrThrow()
        if (playlist.isCustomImage) return

        val firstData = audioMediaRepository.getFirstAudioMedia(playlistId).getOrNull()
        if (firstData == null) {
            playlistRepository.updatePlaylistImage(playlistId, null)
        } else {
            firstData.imageFileFullPath?.let {
                if (it == playlist.imageFileFullPath) return
                playlistRepository.updatePlaylistImage(playlistId, it)
            }
        }
    }
}
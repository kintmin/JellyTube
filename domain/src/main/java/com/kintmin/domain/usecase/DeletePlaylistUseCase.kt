package com.kintmin.domain.usecase

import com.kintmin.domain.repository.PlaylistRepository
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {

    suspend operator fun invoke(playlistId: Int) {
        playlistRepository.deletePlaylist(playlistId)
    }
}
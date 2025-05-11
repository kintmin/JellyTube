package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.repository.PlaylistRepository
import javax.inject.Inject

class AddNewPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(title: String): Result<Unit> {
        return playlistRepository.addPlaylist(title)
    }
}
package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.repository.PlaylistRepository

class ResetPlaylistImageUseCase constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(id: Int): Result<Unit> {
        return playlistRepository.resetPlaylistImage(id)
    }
}

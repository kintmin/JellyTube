package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.repository.PlaylistRepository

class UpdatePlaylistImageUseCase constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(id: Int, imageData: ByteArray): Result<Unit> {
        return playlistRepository.setCustomPlaylistImage(id, imageData)
    }
}

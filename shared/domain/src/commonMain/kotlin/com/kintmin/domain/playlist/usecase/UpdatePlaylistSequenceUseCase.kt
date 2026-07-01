package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.repository.PlaylistRepository

class UpdatePlaylistSequenceUseCase constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(orderedPlaylistIds: List<Int>): Result<Unit> {
        return playlistRepository.updatePlaylistSequences(orderedPlaylistIds)
    }
}

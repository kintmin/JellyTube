package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.repository.PlaylistRepository

class UpdatePlaylistTitleUseCase constructor(
    private val playlistRepository: PlaylistRepository,
){
    suspend operator fun invoke(id: Int, newTitle: String): Result<Unit> {
        return playlistRepository.updatePlaylist(
            id = id,
            name = newTitle,
        )
    }
}
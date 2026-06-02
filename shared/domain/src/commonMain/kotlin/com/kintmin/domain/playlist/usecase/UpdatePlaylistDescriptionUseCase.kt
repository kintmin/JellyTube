package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.repository.PlaylistRepository

class UpdatePlaylistDescriptionUseCase constructor(
    private val playlistRepository: PlaylistRepository,
){
    suspend operator fun invoke(id: Int, newDescription: String): Result<Unit> {
        return playlistRepository.updatePlaylist(
            id = id,
            description = newDescription,
        )
    }
}
package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.repository.PlaylistRepository
import javax.inject.Inject

class UpdatePlaylistDescriptionUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
){
    suspend operator fun invoke(id: Int, newDescription: String): Result<Unit> {
        return playlistRepository.updatePlaylist(
            id = id,
            description = newDescription,
        )
    }
}
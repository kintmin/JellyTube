package com.kintmin.domain.usecase

import com.kintmin.domain.repository.PlaylistRepository
import javax.inject.Inject

class UpdatePlaylistDescriptionUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
){
    suspend operator fun invoke(id: Int, newTitle: String): Result<Unit> {
        return playlistRepository.updatePlaylistDescription(id, newTitle)
    }
}
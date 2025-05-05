package com.kintmin.domain.usecase

import com.kintmin.domain.repository.PlaylistRepository
import javax.inject.Inject

class UpdatePlaylistTitleUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
){
    suspend operator fun invoke(id: Int, newTitle: String): Result<Unit> {
        return playlistRepository.updatePlaylistName(id, newTitle)
    }
}
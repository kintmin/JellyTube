package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.repository.PlaylistRepository

class EnsureSystemPlaylistsUseCase constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(): Result<Unit> = playlistRepository.ensureSystemPlaylists()
}

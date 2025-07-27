package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import javax.inject.Inject

class AddNewPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val log: Log,
) {
    suspend operator fun invoke(title: String): Result<Int> {
        return playlistRepository.addPlaylist(title).onSuccess { id ->
            log.sendFirebaseEvent(FirebaseEvent.AddPlaylist(id, title))
        }
    }
}
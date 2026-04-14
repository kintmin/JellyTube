package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.FirebaseEvent
import javax.inject.Inject

class AddNewPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(title: String): Result<Int> {
        return playlistRepository.addPlaylist(title).onSuccess { id ->
            appLog.sendFirebaseEvent(FirebaseEvent.AddPlaylist(id, title))
        }
    }
}

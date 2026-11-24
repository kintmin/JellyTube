package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.SentryEvent
import com.kintmin.log.secret

class AddNewPlaylistUseCase constructor(
    private val playlistRepository: PlaylistRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(title: String, type: PlaylistType = PlaylistType.USER): Result<Int> {
        return playlistRepository.addPlaylist(title, type).onSuccess { id ->
            appLog.sendSentryEvent(SentryEvent.AddPlaylist(id, secret(title, type.isSecret)))
        }
    }
}

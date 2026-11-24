package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog

/** 한 번만 읽는다. 계속 보려면 [FetchPlaylistFlowUseCase] 다. */
class GetPlaylistUseCase constructor(
    private val playlistRepository: PlaylistRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(playlistId: Int): Result<Playlist> =
        playlistRepository.getPlaylist(playlistId)
            .onFailure { appLog.sendErrorLog(ErrorLog("GetPlaylistUseCase", "플레이리스트 읽기 실패: id=$playlistId", it)) }
}

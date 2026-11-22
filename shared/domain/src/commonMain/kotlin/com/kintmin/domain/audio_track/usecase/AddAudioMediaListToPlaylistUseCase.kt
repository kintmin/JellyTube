package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog

class AddAudioMediaListToPlaylistUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Int> {
        return audioTrackRepository.addCustomAudioTrack(playlistId, audioMediaIdList)
            .onSuccess { appLog.sendDebugLog(DebugLog("PlaylistTrack", "담기: playlistId=$playlistId ids=$audioMediaIdList")) }
            .onFailure { appLog.sendErrorLog(ErrorLog("PlaylistTrack", "담기 실패: playlistId=$playlistId ids=$audioMediaIdList", it)) }
    }
}
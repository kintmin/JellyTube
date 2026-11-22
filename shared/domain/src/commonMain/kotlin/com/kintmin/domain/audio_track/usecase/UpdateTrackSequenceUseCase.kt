package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog

class UpdateTrackSequenceUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaId: Int, oldSequence: Int, newSequence: Int): Result<Unit> {
        return audioTrackRepository.updateTrackSequence(playlistId, audioMediaId, oldSequence, newSequence)
            .onFailure { appLog.sendErrorLog(ErrorLog("PlaylistTrack", "순서 바꾸기 실패: playlistId=$playlistId id=$audioMediaId", it)) }
    }
}
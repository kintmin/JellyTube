package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog

class ToggleFavoriteUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(audioMediaId: Int, isFavorite: Boolean): Result<Unit> =
        audioTrackRepository.setFavorite(audioMediaId, isFavorite)
            .onSuccess { appLog.sendDebugLog(DebugLog("Favorite", "즐겨찾기 ${if (isFavorite) "켬" else "끔"}: id=$audioMediaId")) }
            .onFailure { appLog.sendErrorLog(ErrorLog("Favorite", "즐겨찾기 바꾸기 실패: id=$audioMediaId", it)) }
}

package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.model.SentryEvent

class DeleteAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        source: String,
    ): Result<Unit> = audioMediaRepository.deleteAudioMedia(audioMediaId).onSuccess {
        appLog.sendSentryEvent(SentryEvent.DeleteAudioMedia(source, audioMediaId))
    }.onFailure {
        appLog.sendErrorLog(ErrorLog("DeleteAudioMediaUseCase", "곡 삭제 실패: id=$audioMediaId", it))
    }
}

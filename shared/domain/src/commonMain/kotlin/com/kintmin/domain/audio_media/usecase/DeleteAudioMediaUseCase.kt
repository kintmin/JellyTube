package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.model.SentryEvent
import com.kintmin.log.secret
import kotlinx.coroutines.flow.first

class DeleteAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val audioTrackRepository: AudioTrackRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        source: String,
    ): Result<Unit> {
        // 지우고 나면 어느 플레이리스트에 있었는지 알 수 없다.
        val isSecret = audioMediaId in audioTrackRepository.getSecretAudioMediaIdListFlow().first()
        return audioMediaRepository.deleteAudioMedia(audioMediaId).onSuccess {
            appLog.sendSentryEvent(SentryEvent.DeleteAudioMedia(secret(source, isSecret), audioMediaId))
        }.onFailure {
            appLog.sendErrorLog(ErrorLog("DeleteAudioMediaUseCase", "곡 삭제 실패: id=$audioMediaId", it))
        }
    }
}

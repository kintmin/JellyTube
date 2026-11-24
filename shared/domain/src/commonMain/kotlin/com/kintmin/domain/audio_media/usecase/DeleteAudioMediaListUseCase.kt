package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.secret_mode.secretAudioMediaJudge
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.model.SentryEvent
import com.kintmin.log.secret

/**
 * 일부 성공/삭제가 발생할 수 있다.
 */
class DeleteAudioMediaListUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val audioTrackRepository: AudioTrackRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(idList: List<Int>, sourceList: List<String>): Result<Unit> = suspendRunCatching {
        val isSecret = audioTrackRepository.secretAudioMediaJudge(appLog)
        idList.mapIndexed { index, id ->
            audioMediaRepository.deleteAudioMedia(id).getOrThrow()
            appLog.sendSentryEvent(SentryEvent.DeleteAudioMedia(secret(sourceList[index], isSecret(id)), id))
        }
    }.onFailure { appLog.sendErrorLog(ErrorLog("DeleteAudioMediaListUseCase", "곡 여러 개 삭제 실패: ${idList.size}곡 중", it)) }
        .map { }
}

package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog

class RevertAudioEditUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(audioMediaId: Int): Result<AudioMedia> =
        audioEditRepository.revertEditedAudio(audioMediaId = audioMediaId).also { result ->
            appLog.sendDebugLog(
                DebugLog(
                    tag = "RevertAudioEditUseCase",
                    message = "편집본 해제: id $audioMediaId, 결과 ${result.exceptionOrNull() ?: "성공"}",
                )
            )
        }
}

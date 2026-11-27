package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.lyrics.usecase.RefreshEditedAudioLyricsUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog

class RevertAudioEditUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
    private val refreshEditedAudioLyricsUseCase: RefreshEditedAudioLyricsUseCase,
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
            // 편집 배치가 지워졌으므로 편집본 가사도 떨어져 나간다. 원본 가사는 그대로다.
            result.onSuccess { refreshEditedAudioLyricsUseCase(audioMediaId) }
        }
}

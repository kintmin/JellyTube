package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.domain.audio_edit.repository.AudioEditRepository

class FetchAudioEditTimelineUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(audioMediaId: Int): Result<AudioEditTimeline?> =
        audioEditRepository.fetchEditTimeline(audioMediaId)
            .onFailure { appLog.sendErrorLog(ErrorLog("FetchAudioEditTimelineUseCase", "편집 상태 읽기 실패: id=$audioMediaId", it)) }
}

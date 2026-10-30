package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import kotlin.time.measureTimedValue

class ApplyAudioEditUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        timeline: AudioEditTimeline,
    ): Result<AudioMedia> {
        if (!timeline.hasAudio) {
            return Result.failure(EmptyAudioEditSegment())
        }

        val timedValue = measureTimedValue {
            audioEditRepository.applyEditedAudio(
                audioMediaId = audioMediaId,
                timeline = timeline,
            )
        }

        appLog.sendDebugLog(
            DebugLog(
                tag = "ApplyAudioEditUseCase",
                message = "편집본 적용: 조각 ${timeline.clips.size}개, 소요 ${timedValue.duration}, " +
                    "결과 ${timedValue.value.exceptionOrNull() ?: "성공"}",
            )
        )
        return timedValue.value
    }
}

class EmptyAudioEditSegment(
    override val message: String = "소리가 남은 조각이 하나 이상 있어야 합니다."
) : Throwable()

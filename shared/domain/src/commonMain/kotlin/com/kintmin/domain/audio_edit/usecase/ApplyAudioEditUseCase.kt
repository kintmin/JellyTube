package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.model.AudioEditSegment
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
        segments: List<AudioEditSegment>,
    ): Result<AudioMedia> {
        if (segments.isEmpty()) {
            return Result.failure(EmptyAudioEditSegment())
        }

        val timedValue = measureTimedValue {
            audioEditRepository.applyEditedAudio(
                audioMediaId = audioMediaId,
                segments = segments,
            )
        }

        appLog.sendDebugLog(
            DebugLog(
                tag = "ApplyAudioEditUseCase",
                message = "편집본 적용: 구간 ${segments.size}개, 소요 ${timedValue.duration}, " +
                    "결과 ${timedValue.value.exceptionOrNull() ?: "성공"}",
            )
        )
        return timedValue.value
    }
}

class EmptyAudioEditSegment(
    override val message: String = "남길 구간을 하나 이상 선택해주세요."
) : Throwable()

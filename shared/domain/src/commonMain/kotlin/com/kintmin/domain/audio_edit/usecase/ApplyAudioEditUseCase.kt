package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.model.EmptyAudioEditSegment
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.lyrics.usecase.RefreshEditedAudioLyricsUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import kotlin.time.measureTimedValue

class ApplyAudioEditUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
    private val refreshEditedAudioLyricsUseCase: RefreshEditedAudioLyricsUseCase,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        timeline: AudioEditTimeline,
    ): Result<AudioMedia> {
        audioEditRepository.withSavingMark(audioMediaId) {
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
                    message = "편집본 적용: id $audioMediaId, 조각 ${timeline.clips.size}개, 소요 ${timedValue.duration}, " +
                        "결과 ${timedValue.value.exceptionOrNull() ?: "성공"}",
                )
            )
            // 가사의 sync 는 원본 시간축이라 편집본에는 맞지 않는다. 새 배치로 다시 만든다.
            timedValue.value.onSuccess { refreshEditedAudioLyricsUseCase(audioMediaId) }
            return timedValue.value
        }
    }
}

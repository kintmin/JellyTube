package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository

/** 파일의 실제 길이(ms). DB 길이는 초 단위라 편집에서 끝 1초 미만이 빠지지 않도록 이 값을 쓴다. */
class MeasureAudioDurationUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    suspend operator fun invoke(audioFileFullPath: String): Result<Long> =
        audioEditRepository.measureAudioDurationMs(audioFileFullPath)
}

package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository

class MeasureAudioDurationUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    suspend operator fun invoke(audioFileFullPath: String): Result<Long> =
        audioEditRepository.measureAudioDurationMs(audioFileFullPath)
}

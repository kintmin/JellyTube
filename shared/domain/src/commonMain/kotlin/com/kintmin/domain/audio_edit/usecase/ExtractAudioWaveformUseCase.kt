package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository

class ExtractAudioWaveformUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    suspend operator fun invoke(
        audioFileFullPath: String,
        bucketCount: Int,
    ): Result<List<Int>> = audioEditRepository.extractWaveform(
        audioFileFullPath = audioFileFullPath,
        bucketCount = bucketCount,
    )
}

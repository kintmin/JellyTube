package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.model.AudioWaveform
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import kotlinx.coroutines.flow.Flow

class ExtractAudioWaveformUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    operator fun invoke(
        audioFileFullPath: String,
        bucketCount: Int,
    ): Flow<AudioWaveform> = audioEditRepository.extractWaveform(
        audioFileFullPath = audioFileFullPath,
        bucketCount = bucketCount,
    )
}

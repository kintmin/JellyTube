package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

class ResetAudioMediaImageUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(id: Int): Result<Unit> {
        return audioMediaRepository.clearCustomImage(id)
    }
}

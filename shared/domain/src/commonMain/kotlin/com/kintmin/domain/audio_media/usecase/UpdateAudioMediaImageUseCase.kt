package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

class UpdateAudioMediaImageUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(id: Int, imageData: ByteArray): Result<Unit> {
        return audioMediaRepository.setCustomImage(id, imageData)
    }
}

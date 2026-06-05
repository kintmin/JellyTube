package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

class SaveAudioMediaImageUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(imageData: ByteArray) = audioMediaRepository.saveImage(imageData)
}

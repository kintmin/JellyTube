package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import javax.inject.Inject

class SaveAudioMediaImageUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(imageData: ByteArray) = audioMediaRepository.saveImage(imageData)
}

package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

class UpdateAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(
        id: Int,
        name: String? = null,
        artist: String? = null,
        description: String? = null,
        imageFileFullPath: String? = null,
    ) = audioMediaRepository.updateAudioMedia(
        id = id,
        name = name,
        artist = artist,
        description = description,
        imageFileFullPath = imageFileFullPath,
    )
}
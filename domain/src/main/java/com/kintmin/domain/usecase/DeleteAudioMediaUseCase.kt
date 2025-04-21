package com.kintmin.domain.usecase

import com.kintmin.domain.repository.AudioMediaRepository
import javax.inject.Inject

class DeleteAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return audioMediaRepository.deleteAudioMedia(id)
    }
}
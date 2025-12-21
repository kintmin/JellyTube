package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import javax.inject.Inject

class DeleteAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val log: Log,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        source: String,
    ): Result<Unit> = audioMediaRepository.deleteAudioMedia(audioMediaId).onSuccess {
        log.sendFirebaseEvent(FirebaseEvent.DeleteAudioMedia(source))
    }
}
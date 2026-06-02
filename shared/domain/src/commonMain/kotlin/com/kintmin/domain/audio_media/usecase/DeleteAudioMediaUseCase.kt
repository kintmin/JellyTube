package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.FirebaseEvent

class DeleteAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        source: String,
    ): Result<Unit> = audioMediaRepository.deleteAudioMedia(audioMediaId).onSuccess {
        appLog.sendFirebaseEvent(FirebaseEvent.DeleteAudioMedia(source))
    }
}

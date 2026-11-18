package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.model.AudioEditAlreadySaving
import com.kintmin.domain.audio_edit.repository.AudioEditRepository

class ReserveAudioEditSaveUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    operator fun invoke(audioMediaId: Int): Result<Unit> =
        if (audioEditRepository.reserveSaving(audioMediaId)) {
            Result.success(Unit)
        } else {
            Result.failure(AudioEditAlreadySaving())
        }
}

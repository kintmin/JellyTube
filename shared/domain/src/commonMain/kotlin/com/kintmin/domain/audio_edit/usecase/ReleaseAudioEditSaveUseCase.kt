package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository

class ReleaseAudioEditSaveUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    operator fun invoke(audioMediaId: Int) = audioEditRepository.releaseSaving(audioMediaId)
}

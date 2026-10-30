package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveAudioEditSavingUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    operator fun invoke(audioMediaId: Int): Flow<Boolean> =
        audioEditRepository.observeSavingAudioMediaIds()
            .map { audioMediaId in it }
            .distinctUntilChanged()
}

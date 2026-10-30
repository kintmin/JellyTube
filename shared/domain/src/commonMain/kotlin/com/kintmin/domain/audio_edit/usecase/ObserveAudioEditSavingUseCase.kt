package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** [audioMediaId] 음원의 편집본을 지금 저장하고 있는지. 저장 중에는 다시 저장하거나 되돌릴 수 없다. */
class ObserveAudioEditSavingUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    operator fun invoke(audioMediaId: Int): Flow<Boolean> =
        audioEditRepository.observeSavingAudioMediaIds()
            .map { audioMediaId in it }
            .distinctUntilChanged()
}

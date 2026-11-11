package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import kotlinx.coroutines.flow.Flow

class FetchSavingAudioMediaIdsFlowUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    operator fun invoke(): Flow<Set<Int>> = audioEditRepository.observeSavingAudioMediaIds()
}

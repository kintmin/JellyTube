package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import kotlinx.coroutines.flow.Flow

class FetchAllAudioMediaListFlowUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
) {
    operator fun invoke(): Flow<List<AudioMedia>> = audioMediaRepository.getAudioMediaListFlow()
}

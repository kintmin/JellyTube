package com.kintmin.domain.usecase

import com.kintmin.domain.model.AudioMedia
import com.kintmin.domain.repository.AudioMediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchAudioMediaListFlowUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
) {
    operator fun invoke(playlistId: Int): Flow<List<AudioMedia>> {
        return audioMediaRepository.getAudioMediaListFlow(playlistId)
    }
}
package com.kintmin.domain.usecase

import com.kintmin.domain.repository.PlaybackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchIsPlaybackRepeatingFlowUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
) {
    operator fun invoke(): Flow<Boolean> {
        return playbackRepository.getIsPlaybackRepeatingFlow()
    }
}
package com.kintmin.domain.usecase

import com.kintmin.domain.repository.PlaybackRepository
import javax.inject.Inject

class UpdateIsPlaybackShufflingUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
) {
    suspend operator fun invoke(isShuffling: Boolean): Result<Unit> {
        return playbackRepository.setIsPlaybackShuffling(isShuffling)
    }
}
package com.kintmin.domain.usecase

import com.kintmin.domain.repository.PlaybackRepository
import javax.inject.Inject

class UpdatePlaybackRepeatingUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
) {
    suspend operator fun invoke(isRepeating: Boolean): Result<Unit> {
        return playbackRepository.setPlaybackRepeating(isRepeating)
    }
}
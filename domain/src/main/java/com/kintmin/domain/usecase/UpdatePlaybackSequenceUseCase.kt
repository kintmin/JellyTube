package com.kintmin.domain.usecase

import com.kintmin.domain.repository.PlaybackRepository
import javax.inject.Inject

class UpdatePlaybackSequenceUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository
) {
    suspend operator fun invoke(playlistId: Int, audioMediaId: Int, newSequence: Int) {
        playbackRepository.updatePlaybackSequence(playlistId, audioMediaId, newSequence)
    }
}
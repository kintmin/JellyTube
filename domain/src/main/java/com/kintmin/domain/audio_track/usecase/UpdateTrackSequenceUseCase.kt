package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import javax.inject.Inject

class UpdateTrackSequenceUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaId: Int, newSequence: Int): Result<Unit> {
        return audioTrackRepository.updateTrackSequence(playlistId, audioMediaId, newSequence)
    }
}
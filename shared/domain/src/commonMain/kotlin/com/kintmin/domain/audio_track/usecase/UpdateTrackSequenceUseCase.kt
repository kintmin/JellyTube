package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository

class UpdateTrackSequenceUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaId: Int, oldSequence: Int, newSequence: Int): Result<Unit> {
        return audioTrackRepository.updateTrackSequence(playlistId, audioMediaId, oldSequence, newSequence)
    }
}
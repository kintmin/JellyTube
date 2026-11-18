package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository

class ToggleFavoriteUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(audioMediaId: Int, isFavorite: Boolean): Result<Unit> =
        audioTrackRepository.setFavorite(audioMediaId, isFavorite)
}

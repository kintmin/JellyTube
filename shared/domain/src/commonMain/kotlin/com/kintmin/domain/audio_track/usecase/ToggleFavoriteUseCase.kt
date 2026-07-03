package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository

// 즐겨찾기 on/off 전용 UseCase. 미분류 분류와 직교하게 동작한다.
class ToggleFavoriteUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(audioMediaId: Int, isFavorite: Boolean): Result<Unit> =
        audioTrackRepository.setFavorite(audioMediaId, isFavorite)
}

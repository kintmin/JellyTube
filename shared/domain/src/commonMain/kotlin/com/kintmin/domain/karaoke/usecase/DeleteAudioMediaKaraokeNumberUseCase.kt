package com.kintmin.domain.karaoke.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

/**
 * 현재 음원에 연동된 노래방 번호를 해제한다.
 */
class DeleteAudioMediaKaraokeNumberUseCase(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(audioMediaId: Int): Result<Unit> =
        audioMediaRepository.clearKaraokeNumber(audioMediaId)
}

package com.kintmin.domain.karaoke.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

/**
 * 선택한 노래방 번호를 현재 음원에 연동한다.
 */
class ApplyKaraokeNumberToAudioMediaUseCase(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(audioMediaId: Int, tjKaraokeNumber: String): Result<Unit> =
        audioMediaRepository.updateKaraokeNumber(audioMediaId, tjKaraokeNumber)
}

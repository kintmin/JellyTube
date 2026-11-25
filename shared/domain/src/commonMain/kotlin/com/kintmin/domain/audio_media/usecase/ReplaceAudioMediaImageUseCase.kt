package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

/**
 * 정본 썸네일을 다시 정한다. PC 가 보낸 것은 잘못 넣은 것을 고치는 것이므로 그 자리에서 정본이
 * 되어야 한다 — 앱에서 고른 썸네일이 가리면 바꾼 것이 보이지 않는다.
 */
class ReplaceAudioMediaImageUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(id: Int, imageData: ByteArray): Result<Unit> {
        return audioMediaRepository.replaceImage(id, imageData)
    }
}

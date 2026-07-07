package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

class DeleteAudioMediaLyricsUseCase(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(audioMediaId: Int): Result<Unit> =
        audioMediaRepository.deleteLyrics(audioMediaId)
}

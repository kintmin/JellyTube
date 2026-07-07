package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository

class GetAudioMediaLyricsUseCase(
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(lyricFileFullPath: String): Result<String> =
        audioMediaRepository.getLyrics(lyricFileFullPath)
}

package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.model.LyricsVariant

/**
 * 변형(번역/음차) 가사 파일을 읽어 파싱한다. 파일이 없으면 null.
 */
class GetLyricsVariantUseCase(
    private val audioMediaRepository: AudioMediaRepository,
    private val parseLyricsUseCase: ParseLyricsUseCase,
) {
    suspend operator fun invoke(
        lyricFileFullPath: String,
        variant: LyricsVariant,
    ): Result<List<LyricsLine>?> = runCatching {
        val raw = audioMediaRepository.getVariantLyrics(lyricFileFullPath, variant).getOrThrow()
            ?: return@runCatching null
        parseLyricsUseCase(raw)
    }
}

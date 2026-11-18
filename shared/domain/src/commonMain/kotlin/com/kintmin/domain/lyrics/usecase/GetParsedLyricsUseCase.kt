package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsLine

/** 가사 파일을 읽어 줄로 나눈다. 읽지 못하면 빈 목록이다. */
class GetParsedLyricsUseCase(
    private val getAudioMediaLyricsUseCase: GetAudioMediaLyricsUseCase,
    private val parseLyricsUseCase: ParseLyricsUseCase,
) {
    suspend operator fun invoke(lyricFileFullPath: String): List<LyricsLine> =
        parseLyricsUseCase(getAudioMediaLyricsUseCase(lyricFileFullPath).getOrNull().orEmpty())
}

package com.kintmin.data.repository_impl

import com.kintmin.data.python_bridge.LyricsTransliterator
import com.kintmin.data.translation.LyricsTranslator
import com.kintmin.domain.lyrics.repository.LyricsTranslationRepository

internal class LyricsTranslationRepositoryImpl(
    private val lyricsTranslator: LyricsTranslator,
    private val lyricsTransliterator: LyricsTransliterator,
) : LyricsTranslationRepository {

    override suspend fun translate(text: String, sourceLanguage: String): Result<String> =
        lyricsTranslator.translateToKorean(text, sourceLanguage)

    override suspend fun transliterate(text: String): Result<String> =
        lyricsTransliterator.transliterateToKorean(text)
}

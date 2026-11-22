package com.kintmin.data.translation

import platform.NaturalLanguage.NLLanguageRecognizer
import platform.NaturalLanguage.NLLanguageUndetermined

internal class IosLyricsTranslationEngine(
    private val bridge: IosLyricsTranslatorBridge,
) : LyricsTranslationEngine {

    override suspend fun detectLanguages(lines: List<String>): List<String?> =
        lines.map { line ->
            NLLanguageRecognizer.dominantLanguageForString(line)?.takeUnless { it == NLLanguageUndetermined }
        }

    override suspend fun translateToKorean(lines: List<String>, sourceLanguage: String): List<String> =
        bridge.translateToKorean(lines, sourceLanguage)
}

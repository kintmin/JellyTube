package com.kintmin.data.translation

internal class IosLyricsTranslator(
    private val bridge: IosLyricsTranslatorBridge,
) : LyricsTranslator {

    override suspend fun translateToKorean(text: String, sourceLanguage: String): Result<String> = runCatching {
        val lines = text.split("\n")
        val targets = lines.filter { it.isNotBlank() }
        if (targets.isEmpty()) return@runCatching text

        val translated = bridge.translateToKorean(targets, sourceLanguage)
        check(translated.size == targets.size) {
            "번역 결과 줄 수가 다릅니다: 요청 ${targets.size}, 결과 ${translated.size}"
        }

        val iterator = translated.iterator()
        lines.joinToString("\n") { line -> if (line.isBlank()) line else iterator.next() }
    }
}

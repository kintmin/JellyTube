package com.kintmin.data.translation

/**
 * Apple `Translation`(iOS 18+) 에 번역을 위임한다. 줄 구조 보존은 Android(`MlKitLyricsTranslator`)
 * 와 같은 규칙을 여기서 지킨다 — 빈 줄은 그대로 두고 나머지 줄만 번역한다.
 */
internal class IosLyricsTranslator(
    private val bridge: IosLyricsTranslatorBridge,
) : LyricsTranslator {

    override suspend fun translateToKorean(text: String, sourceLanguage: String): Result<String> = runCatching {
        val lines = text.split("\n")
        val targets = lines.filter { it.isNotBlank() }
        val translated = bridge.translateToKorean(targets, sourceLanguage)
        check(translated.size == targets.size) {
            "번역 결과 줄 수가 다릅니다: 요청 ${targets.size}, 결과 ${translated.size}"
        }

        val iterator = translated.iterator()
        lines.joinToString("\n") { line -> if (line.isBlank()) line else iterator.next() }
    }
}

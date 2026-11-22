package com.kintmin.data.translation

/** 줄마다 원문 언어를 감지해 한국어로 옮긴다. 한국어 줄과 언어를 모르는 줄은 원문 그대로 둔다. */
internal class LyricsTranslator(
    private val engine: LyricsTranslationEngine,
) {

    suspend fun translateToKorean(text: String): Result<String> = runCatching {
        val lines = text.split("\n")
        val targets = lines.withIndex().filter { it.value.isNotBlank() }
        if (targets.isEmpty()) return@runCatching text

        val languages = engine.detectLanguages(targets.map { it.value })
        check(languages.size == targets.size) {
            "언어 감지 결과 줄 수가 다릅니다: 요청 ${targets.size}, 결과 ${languages.size}"
        }

        val result = lines.toMutableList()
        targets.zip(languages)
            .mapNotNull { (line, language) -> language?.takeIf { it != KOREAN }?.let { line to it } }
            .groupBy(keySelector = { it.second }, valueTransform = { it.first })
            .forEach { (language, group) ->
                val translated = engine.translateToKorean(group.map { it.value }, language)
                check(translated.size == group.size) {
                    "번역 결과 줄 수가 다릅니다: 요청 ${group.size}, 결과 ${translated.size}"
                }
                group.zip(translated).forEach { (line, korean) -> result[line.index] = korean }
            }
        result.joinToString("\n")
    }
}

private const val KOREAN = "ko"

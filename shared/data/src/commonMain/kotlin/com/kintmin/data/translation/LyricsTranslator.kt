package com.kintmin.data.translation

import kotlin.coroutines.cancellation.CancellationException

/**
 * 줄마다 원문 언어를 감지해 한국어로 옮긴다. 한국어 줄과 언어를 모르는 줄은 원문 그대로 둔다.
 * 한 언어의 번역이 실패하면 그 줄만 원문으로 두고, 모든 언어가 실패할 때만 실패한다.
 */
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

        // 가나 없이 한자만 있는 줄은 중국어로 감지된다. 줄 위치와 상관없이 곡 안에 일본어 줄이 하나라도
        // 있으면 일본어 곡의 한자 줄로 본다.
        val isJapaneseSong = JAPANESE in languages
        val groups = targets.zip(languages)
            .mapNotNull { (line, language) ->
                when {
                    language == null || language == KOREAN -> null
                    isJapaneseSong && language in CHINESE -> line to JAPANESE
                    else -> line to language
                }
            }
            .groupBy(keySelector = { it.second }, valueTransform = { it.first })

        val result = lines.toMutableList()
        val failures = groups.mapNotNull { (language, group) ->
            try {
                val translated = engine.translateToKorean(group.map { it.value }, language)
                check(translated.size == group.size) {
                    "번역 결과 줄 수가 다릅니다: 요청 ${group.size}, 결과 ${translated.size}"
                }
                group.zip(translated).forEach { (line, korean) -> result[line.index] = korean }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e
            }
        }
        if (groups.isNotEmpty() && failures.size == groups.size) throw failures.first()
        result.joinToString("\n")
    }
}

private const val KOREAN = "ko"
private const val JAPANESE = "ja"

/** ML Kit 은 `zh`, NaturalLanguage 는 `zh-Hans`·`zh-Hant` 로 준다. 병음(`zh-Latn`)은 한자 줄이 아니다. */
private val CHINESE = setOf("zh", "zh-Hans", "zh-Hant")

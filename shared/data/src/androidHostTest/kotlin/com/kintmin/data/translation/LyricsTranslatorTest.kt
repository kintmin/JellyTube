package com.kintmin.data.translation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTranslatorTest {

    private class FakeEngine(
        private val languages: Map<String, String?>,
        private val translate: (List<String>, String) -> List<String> = { lines, language ->
            lines.map { "$language:$it" }
        },
    ) : LyricsTranslationEngine {
        val detectRequests = mutableListOf<List<String>>()
        val translateRequests = mutableListOf<Pair<List<String>, String>>()

        override suspend fun detectLanguages(lines: List<String>): List<String?> {
            detectRequests += lines
            return lines.map { languages[it] }
        }

        override suspend fun translateToKorean(lines: List<String>, sourceLanguage: String): List<String> {
            translateRequests += lines to sourceLanguage
            return translate(lines, sourceLanguage)
        }
    }

    @Test
    fun `줄마다 감지한 언어로 묶어 번역하고 제자리에 둔다`() = runTest {
        val engine = FakeEngine(mapOf("hello" to "en", "君の名は" to "ja", "bye" to "en"))

        val result = LyricsTranslator(engine).translateToKorean("hello\n君の名は\nbye")

        assertEquals(listOf(listOf("hello", "bye") to "en", listOf("君の名は") to "ja"), engine.translateRequests)
        assertEquals("en:hello\nja:君の名は\nen:bye", result.getOrThrow())
    }

    @Test
    fun `한국어 줄과 언어를 모르는 줄은 원문 그대로 둔다`() = runTest {
        val engine = FakeEngine(mapOf("사랑해" to "ko", "la la" to null, "love" to "en"))

        val result = LyricsTranslator(engine).translateToKorean("사랑해\nla la\nlove")

        assertEquals(listOf(listOf("love") to "en"), engine.translateRequests)
        assertEquals("사랑해\nla la\nen:love", result.getOrThrow())
    }

    @Test
    fun `빈 줄은 감지·번역에 넘기지 않고 제자리에 둔다`() = runTest {
        val engine = FakeEngine(mapOf("a" to "en", "b" to "en"))

        val result = LyricsTranslator(engine).translateToKorean("a\n\nb\n  ")

        assertEquals(listOf(listOf("a", "b")), engine.detectRequests)
        assertEquals("en:a\n\nen:b\n  ", result.getOrThrow())
    }

    @Test
    fun `빈 줄뿐이면 엔진을 부르지 않는다`() = runTest {
        val engine = FakeEngine(emptyMap())

        val result = LyricsTranslator(engine).translateToKorean("\n  \n")

        assertTrue(engine.detectRequests.isEmpty())
        assertEquals("\n  \n", result.getOrThrow())
    }

    @Test
    fun `곡 안에 일본어 줄이 있으면 중국어로 감지된 한자 줄도 일본어로 번역한다`() = runTest {
        val engine = FakeEngine(mapOf("永遠" to "zh", "君の名は" to "ja", "東京" to "zh-Hans"))

        val result = LyricsTranslator(engine).translateToKorean("永遠\n君の名は\n東京")

        assertEquals(listOf(listOf("永遠", "君の名は", "東京") to "ja"), engine.translateRequests)
        assertEquals("ja:永遠\nja:君の名は\nja:東京", result.getOrThrow())
    }

    @Test
    fun `제목처럼 앞쪽이 한자 줄뿐이고 일본어 줄이 뒤에야 나와도 일본어로 번역한다`() = runTest {
        val engine = FakeEngine(mapOf("桜吹雪" to "zh", "東京" to "zh-Hant", "hello" to "en", "さよなら" to "ja"))

        val result = LyricsTranslator(engine).translateToKorean("桜吹雪\n東京\nhello\nさよなら")

        assertEquals(
            listOf(listOf("桜吹雪", "東京", "さよなら") to "ja", listOf("hello") to "en"),
            engine.translateRequests,
        )
        assertEquals("ja:桜吹雪\nja:東京\nen:hello\nja:さよなら", result.getOrThrow())
    }

    @Test
    fun `일본어 줄이 없으면 중국어 줄은 중국어로 번역한다`() = runTest {
        val engine = FakeEngine(mapOf("永遠" to "zh", "hello" to "en"))

        val result = LyricsTranslator(engine).translateToKorean("永遠\nhello")

        assertEquals("zh:永遠\nen:hello", result.getOrThrow())
    }

    @Test
    fun `병음은 일본어 곡이어도 일본어로 돌리지 않는다`() = runTest {
        val engine = FakeEngine(mapOf("ni hao" to "zh-Latn", "君の名は" to "ja"))

        val result = LyricsTranslator(engine).translateToKorean("ni hao\n君の名は")

        assertEquals("zh-Latn:ni hao\nja:君の名は", result.getOrThrow())
    }

    @Test
    fun `한 언어의 번역이 실패하면 그 줄만 원문으로 두고 나머지는 번역한다`() = runTest {
        val engine = FakeEngine(mapOf("love" to "en", "oh oh" to "de")) { lines, language ->
            if (language == "de") error("모델 다운로드 실패")
            lines.map { "$language:$it" }
        }

        val result = LyricsTranslator(engine).translateToKorean("love\noh oh")

        assertEquals("en:love\noh oh", result.getOrThrow())
    }

    @Test
    fun `모든 언어의 번역이 실패하면 실패한다`() = runTest {
        val engine = FakeEngine(mapOf("love" to "en", "oh oh" to "de")) { _, _ -> error("앱이 백그라운드에 있음") }

        val result = LyricsTranslator(engine).translateToKorean("love\noh oh")

        assertTrue(result.isFailure)
    }

    @Test
    fun `번역 결과 줄 수가 다르면 실패한다`() = runTest {
        val engine = FakeEngine(mapOf("a" to "en", "b" to "en")) { lines, _ -> lines.drop(1) }

        val result = LyricsTranslator(engine).translateToKorean("a\nb")

        assertTrue(result.isFailure)
    }
}

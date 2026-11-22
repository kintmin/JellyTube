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
    fun `번역 결과 줄 수가 다르면 실패한다`() = runTest {
        val engine = FakeEngine(mapOf("a" to "en", "b" to "en")) { lines, _ -> lines.drop(1) }

        val result = LyricsTranslator(engine).translateToKorean("a\nb")

        assertTrue(result.isFailure)
    }
}

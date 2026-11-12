package com.kintmin.data.translation

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosLyricsTranslatorTest {

    private class FakeBridge(
        private val translate: (List<String>) -> List<String>,
    ) : IosLyricsTranslatorBridge {
        val requests = mutableListOf<List<String>>()

        override suspend fun translateToKorean(lines: List<String>, sourceLanguage: String): List<String> {
            requests += lines
            return translate(lines)
        }
    }

    @Test
    fun `빈 줄은 번역에 넘기지 않고 제자리에 둔다`() = runTest {
        val bridge = FakeBridge { lines -> lines.map { "번역:$it" } }

        val result = IosLyricsTranslator(bridge).translateToKorean("a\n\nb\n  \nc", "en")

        assertEquals(listOf("a", "b", "c"), bridge.requests.single())
        assertEquals("번역:a\n\n번역:b\n  \n번역:c", result.getOrThrow())
    }

    @Test
    fun `결과 줄 수가 다르면 실패한다`() = runTest {
        val bridge = FakeBridge { lines -> lines.drop(1) }

        val result = IosLyricsTranslator(bridge).translateToKorean("a\nb", "ja")

        assertTrue(result.isFailure)
    }
}

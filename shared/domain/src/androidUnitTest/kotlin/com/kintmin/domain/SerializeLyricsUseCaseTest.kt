package com.kintmin.domain

import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.ParseLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SerializeLyricsUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class SerializeLyricsUseCaseTest {

    private val serialize = SerializeLyricsUseCase()
    private val parse = ParseLyricsUseCase()

    @Test
    fun `timeMs를 M SS cc 형식 태그로 직렬화한다`() {
        val text = serialize(
            listOf(
                LyricsLine(timeMs = 1000L, text = "첫 줄"),
                LyricsLine(timeMs = (62 * 60 + 3) * 1000L + 450L, text = "긴 줄"),
            ),
        )

        assertEquals("[00:01.00]첫 줄\n[62:03.45]긴 줄", text)
    }

    @Test
    fun `timeMs가 null이면 0으로 직렬화한다`() {
        val text = serialize(listOf(LyricsLine(timeMs = null, text = "가사")))
        assertEquals("[00:00.00]가사", text)
    }

    @Test
    fun `한 줄에 개행이 있으면 같은 시간으로 여러 라인으로 전개한다`() {
        val text = serialize(listOf(LyricsLine(timeMs = 2000L, text = "a\nb")))
        assertEquals("[00:02.00]a\n[00:02.00]b", text)
    }

    @Test
    fun `50ms 단위 값은 parse-serialize 왕복이 동등하다`() {
        val original = listOf(
            LyricsLine(timeMs = 1050L, text = "첫 줄"),
            LyricsLine(timeMs = 3500L, text = "둘째 줄"),
            LyricsLine(timeMs = (10 * 60 + 12) * 1000L + 300L, text = "셋째 줄"),
        )

        val roundTrip = parse(serialize(original))

        assertEquals(original, roundTrip)
    }
}

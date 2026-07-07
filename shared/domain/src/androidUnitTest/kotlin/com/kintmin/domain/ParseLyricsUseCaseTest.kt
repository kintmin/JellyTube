package com.kintmin.domain

import com.kintmin.domain.lyrics.usecase.ParseLyricsUseCase
import com.kintmin.domain.lyrics.usecase.activeLyricIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseLyricsUseCaseTest {

    private val useCase = ParseLyricsUseCase()

    @Test
    fun `LRC 타임태그를 파싱해 timeMs와 텍스트를 추출한다`() {
        val lines = useCase("[00:01.00] 첫 줄\n[00:03.50] 둘째 줄")

        assertEquals(2, lines.size)
        assertEquals(1000L, lines[0].timeMs)
        assertEquals("첫 줄", lines[0].text)
        assertEquals(3500L, lines[1].timeMs)
        assertEquals("둘째 줄", lines[1].text)
    }

    @Test
    fun `타임태그가 없으면 모든 줄을 timeMs=null로 반환한다`() {
        val lines = useCase("첫 줄\n둘째 줄")

        assertEquals(2, lines.size)
        assertNull(lines[0].timeMs)
        assertNull(lines[1].timeMs)
        assertEquals("첫 줄", lines[0].text)
    }

    @Test
    fun `한 줄에 여러 타임태그가 있으면 각각 별도 줄로 확장한다`() {
        val lines = useCase("[00:01.00][00:02.00] 후렴")

        assertEquals(2, lines.size)
        assertEquals(1000L, lines[0].timeMs)
        assertEquals(2000L, lines[1].timeMs)
        assertTrue(lines.all { it.text == "후렴" })
    }

    @Test
    fun `메타 태그와 빈 줄은 싱크 모드에서 무시한다`() {
        val lines = useCase("[ar:아이유]\n[ti:밤편지]\n[00:01.00] 가사")

        assertEquals(1, lines.size)
        assertEquals(1000L, lines[0].timeMs)
        assertEquals("가사", lines[0].text)
    }

    @Test
    fun `현재 위치에 맞는 활성 줄 인덱스를 계산한다`() {
        val lines = useCase("[00:01.00] a\n[00:03.00] b\n[00:05.00] c")

        assertEquals(-1, activeLyricIndex(lines, 500))   // 첫 줄 이전
        assertEquals(0, activeLyricIndex(lines, 1000))   // 첫 줄 경계
        assertEquals(0, activeLyricIndex(lines, 2999))
        assertEquals(1, activeLyricIndex(lines, 3000))
        assertEquals(2, activeLyricIndex(lines, 9999))   // 마지막 줄 이후
    }

    @Test
    fun `비싱크 가사는 활성 인덱스가 항상 -1`() {
        val lines = useCase("가사 한 줄\n가사 두 줄")
        assertEquals(-1, activeLyricIndex(lines, 5000))
    }
}

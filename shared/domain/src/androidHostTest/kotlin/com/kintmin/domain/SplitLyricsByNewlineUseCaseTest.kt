package com.kintmin.domain

import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.SplitLyricsByNewlineUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitLyricsByNewlineUseCaseTest {

    private val useCase = SplitLyricsByNewlineUseCase()

    @Test
    fun `개행이 없는 줄은 그대로 유지한다`() {
        val result = useCase(
            listOf(LyricsLine(timeMs = 0L, text = "a"), LyricsLine(timeMs = 5000L, text = "b")),
            audioEndMs = 10000L,
        )

        assertEquals(
            listOf(LyricsLine(0L, "a"), LyricsLine(5000L, "b")),
            result,
        )
    }

    @Test
    fun `개행이 있는 줄을 다음 줄 시작까지 1_n 균등 배분한다`() {
        val result = useCase(
            listOf(LyricsLine(timeMs = 0L, text = "a\nb"), LyricsLine(timeMs = 10000L, text = "c")),
            audioEndMs = 20000L,
        )

        assertEquals(
            listOf(LyricsLine(0L, "a"), LyricsLine(5000L, "b"), LyricsLine(10000L, "c")),
            result,
        )
    }

    @Test
    fun `마지막 줄의 개행은 음원 종료 시각 기준으로 배분한다`() {
        val result = useCase(
            listOf(LyricsLine(timeMs = 0L, text = "only"), LyricsLine(timeMs = 4000L, text = "x\ny\nz")),
            audioEndMs = 10000L,
        )

        assertEquals(
            listOf(
                LyricsLine(0L, "only"),
                LyricsLine(4000L, "x"),
                LyricsLine(6000L, "y"),
                LyricsLine(8000L, "z"),
            ),
            result,
        )
    }
}

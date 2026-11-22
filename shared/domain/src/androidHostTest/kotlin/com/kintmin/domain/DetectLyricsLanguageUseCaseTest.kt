package com.kintmin.domain

import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.DetectLyricsLanguageUseCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectLyricsLanguageUseCaseTest {

    private val useCase = DetectLyricsLanguageUseCase()

    private fun lines(vararg text: String) = text.map { LyricsLine(timeMs = null, text = it) }

    @Test
    fun `영어 가사는 한국어도 일본어도 없다`() {
        val result = useCase(lines("Hello world", "Take me home"))

        assertFalse(result.hasKorean)
        assertFalse(result.hasJapanese)
    }

    @Test
    fun `일본어 가나가 있으면 일본어로 판별한다`() {
        // の, は 는 히라가나, カ 는 가타카나 → 일본어 신호
        val result = useCase(lines("君の名は", "カタカナ"))

        assertTrue(result.hasJapanese)
        assertFalse(result.hasKorean)
    }

    @Test
    fun `한글이 있으면 한국어로 판별한다`() {
        val result = useCase(lines("사랑해요 그대여"))

        assertTrue(result.hasKorean)
        assertFalse(result.hasJapanese)
    }
}

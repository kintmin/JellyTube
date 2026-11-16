package com.kintmin.domain

import com.kintmin.domain.lyrics.usecase.BuildLyricsSearchQueryUseCase
import com.kintmin.domain.lyrics.usecase.MatchLyricsTrackNameUseCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchLyricsTrackNameUseCaseTest {

    private val useCase = MatchLyricsTrackNameUseCase(BuildLyricsSearchQueryUseCase())

    @Test
    fun `제목 앞뒤에 다른 단어가 있어도 곡명이 연속으로 들어 있으면 일치한다`() {
        assertTrue(useCase("AAA BBB - don't stop me now DDD", "Don't Stop Me Now"))
    }

    @Test
    fun `곡명 괄호 안의 다른 언어 제목으로도 일치한다`() {
        assertTrue(useCase("IU 밤편지 (Official MV)", "Through the Night (밤편지)"))
        assertTrue(useCase("IU Through the Night", "Through the Night (밤편지)"))
    }

    @Test
    fun `다른 단어 속 일부는 일치로 보지 않는다`() {
        assertFalse(useCase("Cleopatra", "Leo"))
    }

    @Test
    fun `곡명 순서가 다르면 일치하지 않는다`() {
        assertFalse(useCase("Me Stop", "Stop Me"))
    }

    @Test
    fun `곡명이 없거나 정제 후 비면 일치하지 않는다`() {
        assertFalse(useCase("Leo", null))
        assertFalse(useCase("Leo", " "))
        assertFalse(useCase("MV Leo", "(MV)"))
    }
}

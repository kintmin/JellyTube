package com.kintmin.domain

import com.kintmin.domain.lyrics.usecase.BuildLyricsSearchQueryUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildLyricsSearchQueryUseCaseTest {

    private val useCase = BuildLyricsSearchQueryUseCase()

    @Test
    fun `괄호와 그 내용을 제거하고 앞 3단어만 남긴다`() {
        assertEquals("sample to sample", useCase("sample(ft. temp) to sample"))
    }

    @Test
    fun `단어가 3개를 넘으면 앞 3단어만 사용한다`() {
        assertEquals("one two three", useCase("one two three four five"))
    }

    @Test
    fun `대괄호_중괄호_전각괄호도 제거한다`() {
        assertEquals("song title", useCase("song [MV] title （Official）{live}"))
    }

    @Test
    fun `중복 공백을 정규화한다`() {
        assertEquals("a b c", useCase("  a   b  c  d "))
    }

    @Test
    fun `빈 문자열은 빈 검색어가 된다`() {
        assertEquals("", useCase("   "))
    }
}

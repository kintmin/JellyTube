package com.kintmin.domain

import com.kintmin.domain.lyrics.usecase.BuildLyricsSearchQueryUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildLyricsSearchQueryUseCaseTest {

    private val useCase = BuildLyricsSearchQueryUseCase()

    @Test
    fun `괄호와 그 내용을 제거한다`() {
        assertEquals("sample to sample", useCase("sample(ft. temp) to sample"))
    }

    @Test
    fun `단어 수를 제한하지 않는다`() {
        assertEquals("one two three four five", useCase("one two three four five"))
    }

    @Test
    fun `대괄호_중괄호_전각괄호도 제거한다`() {
        assertEquals("song title", useCase("song [MV] title （Official）{live}"))
    }

    @Test
    fun `중복 공백을 정규화한다`() {
        assertEquals("a b c d", useCase("  a   b  c  d "))
    }

    @Test
    fun `빈 문자열은 빈 검색어가 된다`() {
        assertEquals("", useCase("   "))
    }

    @Test
    fun `구분자만으로 된 단어는 버린다`() {
        assertEquals("IU 밤편지", useCase("IU - 밤편지 (Official MV)"))
        assertEquals("HYBE BTS Butter", useCase("HYBE | BTS 'Butter' Official"))
        assertEquals("IU Palette", useCase("[MV] IU(아이유) _ Palette(팔레트)"))
    }

    @Test
    fun `전각 대괄호는 내용째 제거하고 일본식 괄호는 내용을 남긴다`() {
        assertEquals("YOASOBI アイドル", useCase("【MV】YOASOBI「アイドル」Official Music Video"))
        assertEquals("米津玄師 Lemon", useCase("米津玄師『Lemon』"))
    }

    @Test
    fun `따옴표는 벗기되 단어 안 아포스트로피는 유지한다`() {
        assertEquals("Don't Stop Me Now", useCase("\"Don't Stop Me Now\""))
    }

    @Test
    fun `잡음 단어를 제거한다`() {
        assertEquals("NewJeans Ditty", useCase("NewJeans Ditty MV"))
        assertEquals("아이유 좋은 날", useCase("아이유 가사 좋은 날"))
    }

    @Test
    fun `전각 구분자만으로 된 단어도 버린다`() {
        assertEquals("YOASOBI アイドル", useCase("YOASOBI ／ アイドル"))
        assertEquals("ヨルシカ 花に亡霊", useCase("ヨルシカ － 花に亡霊 ・ 、"))
    }

    @Test
    fun `전각 대괄호도 내용째 제거한다`() {
        assertEquals("Song", useCase("［MV］ Song"))
    }

    @Test
    fun `Music Video 는 단어 경계에서만 제거한다`() {
        assertEquals("Song", useCase("Song Music Video"))
        assertEquals("Song Music Videos", useCase("Song Music Videos"))
    }

    @Test
    fun `하이픈이 붙은 단어는 유지한다`() {
        assertEquals("Spider-Man theme", useCase("Spider-Man theme"))
    }
}

package com.kintmin.presentation

import com.kintmin.presentation.extension.matchKorean
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringExtensionTest {

    @Test
    fun `matchKorean 은 원문과 검색어의 공백을 무시한다`() {
        assertTrue("밤양갱".matchKorean("밤 양갱"))
        assertTrue("밤 양 갱".matchKorean("밤양갱"))
        assertTrue("Love wins all".matchKorean("lovewins"))
    }

    @Test
    fun `matchKorean 은 공백만 있는 검색어를 빈 검색어로 본다`() {
        assertTrue("밤양갱".matchKorean("  "))
    }

    @Test
    fun `matchKorean 은 초성과 받침 없는 글자로도 찾는다`() {
        assertTrue("밤양갱".matchKorean("ㅂㅇㄱ"))
        assertTrue("밤양갱".matchKorean("바"))
        assertFalse("밤양갱".matchKorean("방"))
    }

    @Test
    fun `matchKorean 은 일치가 끊긴 글자부터 다시 맞춰 본다`() {
        assertTrue("aab".matchKorean("ab"))
        assertTrue("가가나".matchKorean("가나"))
    }

    @Test
    fun `matchKorean 은 떨어진 글자를 이어 붙여 맞추지 않는다`() {
        assertFalse("가다나".matchKorean("가나"))
    }
}

package com.kintmin.desktop.upload

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class NaturalFileNameOrderTest {

    private fun sort(vararg names: String) = names.map(::File).sortedWith(naturalFileNameOrder).map { it.name }

    @Test
    fun `숫자 구간은 수로 비교한다`() {
        assertEquals(listOf("2.mp3", "10.mp3", "track 9.mp3", "track 010.mp3"), sort("track 010.mp3", "10.mp3", "track 9.mp3", "2.mp3"))
    }

    @Test
    fun `대소문자는 가리지 않는다`() {
        assertEquals(listOf("a.mp3", "B.mp3", "c.mp3"), sort("c.mp3", "B.mp3", "a.mp3"))
    }

    @Test
    fun `한글은 가나다순이다`() {
        assertEquals(listOf("가.mp3", "나.mp3", "다.mp3"), sort("다.mp3", "가.mp3", "나.mp3"))
    }
}

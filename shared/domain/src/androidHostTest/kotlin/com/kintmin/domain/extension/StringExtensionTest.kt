package com.kintmin.domain.extension

import org.junit.Assert.assertEquals
import org.junit.Test

class StringExtensionTest {

    @Test
    fun `개행을 공백으로 바꾸고 앞뒤 여백은 남긴다`() {
        assertEquals("드라이브 음악 ", "드라이브\n음악 ".flattenNewlines())
    }

    @Test
    fun `이어진 개행은 공백 하나로 묶는다`() {
        assertEquals("드라이브 음악", "드라이브\r\n\n음악".flattenNewlines())
    }

    @Test
    fun `줄을 가르는 유니코드 문자도 공백으로 바꾼다`() {
        listOf(Char(0x85), Char(0x2028), Char(0x2029)).forEach { separator ->
            assertEquals("가 나", "가${separator}나".flattenNewlines())
        }
    }
}

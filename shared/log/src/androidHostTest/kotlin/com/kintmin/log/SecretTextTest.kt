package com.kintmin.log

import org.junit.Assert.assertEquals
import org.junit.Test

class SecretTextTest {

    @Test
    fun `비밀이면 값 대신 가린 글자를 낸다`() {
        assertEquals(HIDDEN_TEXT, secret("https://y/1", isSecret = true))
    }

    @Test
    fun `비밀이 아니면 원문을 낸다`() {
        assertEquals("곡 이름", secret("곡 이름", isSecret = false))
        assertEquals("null", secret(null, isSecret = false))
    }
}

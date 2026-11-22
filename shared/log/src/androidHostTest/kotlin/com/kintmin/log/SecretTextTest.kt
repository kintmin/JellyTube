package com.kintmin.log

import org.junit.Assert.assertEquals
import org.junit.Test

class SecretTextTest {

    private val text = "들여옴: id=1 name=${secret("곡 이름")} url=${secret("https://y/1")}"

    @Test
    fun `숨기면 표시한 값마다 가린 글자로 바꾼다`() {
        assertEquals("들여옴: id=1 name=$HIDDEN_TEXT url=$HIDDEN_TEXT", revealSecrets(text, hide = true))
    }

    @Test
    fun `숨기지 않으면 표시만 걷어 원문을 남긴다`() {
        assertEquals("들여옴: id=1 name=곡 이름 url=https://y/1", revealSecrets(text, hide = false))
    }

    @Test
    fun `표시가 없으면 그대로 둔다`() {
        assertEquals("100% message", revealSecrets("100% message", hide = true))
    }
}

package com.kintmin.domain.audio_media

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioMediaTextLimitTest {

    @Test
    fun `상한을 넘는 글은 상한 길이로 자른다`() {
        assertEquals(100, AudioMediaTextLimit.clampName("가".repeat(255)).length)
        assertEquals(50, AudioMediaTextLimit.clampArtist("a".repeat(51)).length)
        assertEquals(5000, AudioMediaTextLimit.clampDescription("a".repeat(8000)).length)
    }

    @Test
    fun `상한 이하의 글은 그대로 둔다`() {
        assertEquals("밤양갱", AudioMediaTextLimit.clampName("밤양갱"))
        assertEquals("a".repeat(50), AudioMediaTextLimit.clampArtist("a".repeat(50)))
    }

    @Test
    fun `상한에 걸친 서로게이트 쌍은 쪼개지 않고 앞에서 자른다`() {
        val name = "a".repeat(99) + "😀"

        assertEquals("a".repeat(99), AudioMediaTextLimit.clampName(name))
    }
}

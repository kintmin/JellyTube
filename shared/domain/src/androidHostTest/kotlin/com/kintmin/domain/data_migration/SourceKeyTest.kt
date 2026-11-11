package com.kintmin.domain.data_migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SourceKeyTest {

    @Test
    fun `같은 영상의 주소 변형은 같은 키가 된다`() {
        val variants = listOf(
            "https://youtu.be/abc123?si=AAAA",
            "https://youtu.be/abc123?si=BBBB",
            "https://www.youtube.com/watch?v=abc123",
            "https://m.youtube.com/watch?v=abc123&t=10",
            "https://youtube.com/watch?feature=share&v=abc123",
            "https://M.YouTube.com/watch?v=abc123#t=3",
            "https://www.youtube.com/shorts/abc123?si=CCCC",
            "https://www.youtube.com/live/abc123?si=DDDD",
            "https://music.youtube.com/watch?v=abc123&si=EEEE",
        )

        assertEquals(setOf("youtube:abc123"), variants.map(SourceKey::of).toSet())
    }

    @Test
    fun `다른 영상은 다른 키가 된다`() {
        assertNotEquals(
            SourceKey.of("https://youtu.be/abc123"),
            SourceKey.of("https://youtu.be/abc124"),
        )
    }

    @Test
    fun `유튜브 영상 주소가 아니면 원문이 그대로 키다`() {
        listOf(
            "quickShare://sha256/deadbeef",
            "fileShare://sha256/deadbeef",
            "https://notyoutube.com/watch?v=abc123",
            "https://www.youtube.com/watch",
            "https://www.youtube.com/channel/abc123",
        ).forEach { source ->
            assertEquals(source, SourceKey.of(source))
        }
    }
}

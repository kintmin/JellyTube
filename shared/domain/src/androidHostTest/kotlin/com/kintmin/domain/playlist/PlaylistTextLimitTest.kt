package com.kintmin.domain.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistTextLimitTest {

    @Test
    fun `상한을 넘는 글은 상한 길이로 자른다`() {
        assertEquals(PlaylistTextLimit.MAX_TITLE_LENGTH, PlaylistTextLimit.clampTitle("가".repeat(150)).length)
        assertEquals(
            PlaylistTextLimit.MAX_DESCRIPTION_LENGTH,
            PlaylistTextLimit.clampDescription("a".repeat(5000)).length,
        )
    }

    @Test
    fun `상한 이하의 글은 그대로 둔다`() {
        assertEquals("드라이브", PlaylistTextLimit.clampTitle("드라이브"))
        assertEquals("운동할 때 듣는 노래", PlaylistTextLimit.clampDescription("운동할 때 듣는 노래"))
    }

    @Test
    fun `앞뒤 여백을 지우고 가운데 개행은 공백으로 바꾼다`() {
        assertEquals("드라이브 음악", PlaylistTextLimit.clampTitle("\n 드라이브\n음악 \r\n"))
    }

    @Test
    fun `자른 끝의 공백을 지워 두 번 적용해도 결과가 같다`() {
        val once = PlaylistTextLimit.clampTitle("가".repeat(19) + " 나다라")

        assertEquals("가".repeat(19), once)
        assertEquals(once, PlaylistTextLimit.clampTitle(once))
    }

    @Test
    fun `상한에 걸친 서로게이트 쌍은 쪼개지 않고 앞에서 자른다`() {
        assertEquals("a".repeat(19), PlaylistTextLimit.clampTitle("a".repeat(19) + "😀"))
    }
}

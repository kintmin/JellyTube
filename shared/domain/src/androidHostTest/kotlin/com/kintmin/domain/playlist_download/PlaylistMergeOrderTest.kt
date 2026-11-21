package com.kintmin.domain.playlist_download

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistMergeOrderTest {

    @Test
    fun `빠진 곡은 유튜브에서 바로 앞 곡 뒤에, 앞 곡이 없으면 바로 뒤 곡 앞에 끼운다`() {
        assertEquals(listOf(9, 0, 1, 2, 3, 4), PlaylistMergeOrder.of(current = listOf(9, 1, 3), youtubeOrder = listOf(0, 1, 2, 3, 4)))
    }

    @Test
    fun `재생목록에 유튜브 곡이 하나도 없으면 원래 곡 뒤에 유튜브 순서대로 붙인다`() {
        assertEquals(listOf(9, 1, 2), PlaylistMergeOrder.of(current = listOf(9), youtubeOrder = listOf(1, 2)))
    }

    @Test
    fun `이미 있는 곡은 유튜브와 순서가 달라도 옮기지 않는다`() {
        assertEquals(listOf(3, 1, 2), PlaylistMergeOrder.of(current = listOf(3, 1), youtubeOrder = listOf(1, 2, 3)))
    }
}

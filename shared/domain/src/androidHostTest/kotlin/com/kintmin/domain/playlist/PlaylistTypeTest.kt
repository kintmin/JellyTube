package com.kintmin.domain.playlist

import com.kintmin.domain.playlist.model.PlaylistType
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistTypeTest {

    @Test
    fun `비밀 플레이리스트는 시스템 플레이리스트가 아니다`() {
        assertEquals(
            setOf(PlaylistType.TOTAL, PlaylistType.UNCATEGORIZED, PlaylistType.FAVORITE),
            PlaylistType.entries.filter { it.isSystem }.toSet(),
        )
    }

    @Test
    fun `비밀 플레이리스트도 다운로드 대상이 될 수 있다`() {
        assertEquals(
            setOf(PlaylistType.USER, PlaylistType.UNCATEGORIZED, PlaylistType.SECRET),
            PlaylistType.entries.filter { it.canBeDownloadTarget }.toSet(),
        )
    }
}

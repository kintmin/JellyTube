package com.kintmin.domain.playlist

import com.kintmin.domain.playlist.model.PlaylistType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistTypeTest {

    @Test
    fun `비밀 플레이리스트는 시스템 플레이리스트가 아니다`() {
        assertEquals(
            setOf(
                PlaylistType.TOTAL,
                PlaylistType.UNCATEGORIZED,
                PlaylistType.FAVORITE,
                PlaylistType.SECRET_FAVORITE,
                PlaylistType.SECRET_TOTAL,
            ),
            PlaylistType.entries.filter { it.isSystem }.toSet(),
        )
    }

    @Test
    fun `전체는 시스템이 채우고 비우므로 사용자가 넣고 뺄 수 없다`() {
        assertEquals(
            setOf(PlaylistType.TOTAL, PlaylistType.UNCATEGORIZED, PlaylistType.SECRET_TOTAL),
            PlaylistType.entries.filter { it.isAutoManaged }.toSet(),
        )
    }

    @Test
    fun `곡을 전부 담는 자리는 전체와 비밀 전체뿐이다`() {
        assertEquals(
            setOf(PlaylistType.TOTAL, PlaylistType.SECRET_TOTAL),
            PlaylistType.entries.filter { it.containsEveryTrack }.toSet(),
        )
    }

    @Test
    fun `비밀 플레이리스트도 다운로드 대상이 될 수 있다`() {
        assertEquals(
            setOf(PlaylistType.USER, PlaylistType.UNCATEGORIZED, PlaylistType.SECRET),
            PlaylistType.entries.filter { it.canBeDownloadTarget }.toSet(),
        )
    }

    @Test
    fun `비밀 곡의 즐겨찾기와 전체도 비밀 계열이라 비밀모드에서만 보인다`() {
        assertEquals(
            setOf(PlaylistType.SECRET, PlaylistType.SECRET_FAVORITE, PlaylistType.SECRET_TOTAL),
            PlaylistType.entries.filter { it.isSecret }.toSet(),
        )
        PlaylistType.entries.filter { it.isSecret }.forEach {
            assertFalse(it.isVisible(isSecretModeEnabled = false))
            assertTrue(it.isVisible(isSecretModeEnabled = true))
        }
        PlaylistType.entries.filterNot { it.isSecret }.forEach {
            assertTrue(it.isVisible(isSecretModeEnabled = false))
        }
    }
}

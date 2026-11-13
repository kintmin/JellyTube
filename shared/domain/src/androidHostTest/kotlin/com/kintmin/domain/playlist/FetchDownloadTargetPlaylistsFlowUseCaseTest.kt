package com.kintmin.domain.playlist

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchDownloadTargetPlaylistsFlowUseCase
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration

class FetchDownloadTargetPlaylistsFlowUseCaseTest {

    private val useCase = FetchDownloadTargetPlaylistsFlowUseCase(mockk(), mockk())

    private val total = playlist(1, PlaylistType.TOTAL)
    private val uncategorized = playlist(2, PlaylistType.UNCATEGORIZED)
    private val favorite = playlist(3, PlaylistType.FAVORITE)
    private val user = playlist(4, PlaylistType.USER)
    private val all = listOf(total, uncategorized, favorite, user)

    @Test
    fun `사용자 플레이리스트와 미분류만 고를 수 있다`() {
        assertEquals(listOf(uncategorized, user), useCase.resolve(savedId = null, playlists = all).selectable)
    }

    @Test
    fun `저장된 대상이 사용자 플레이리스트면 그대로 쓴다`() {
        assertEquals(user.id, useCase.resolve(savedId = user.id, playlists = all).targetId)
    }

    @Test
    fun `저장된 대상이 전체·즐겨찾기·없는 id·없음이면 미분류로 간다`() {
        listOf(total.id, favorite.id, 999, null).forEach { savedId ->
            assertEquals(uncategorized.id, useCase.resolve(savedId = savedId, playlists = all).targetId)
        }
    }

    @Test
    fun `미분류도 아직 없으면 대상이 없다`() {
        assertNull(useCase.resolve(savedId = total.id, playlists = listOf(total)).targetId)
    }

    private fun playlist(id: Int, type: PlaylistType) = Playlist(
        id = id,
        name = "p$id",
        description = "",
        audioMediaCount = 0,
        playTimeDuration = Duration.ZERO,
        createdTime = LocalDateTime(2026, 1, 1, 0, 0),
        imageFileFullPath = null,
        isCustomImage = false,
        sequence = 0,
        type = type,
    )
}

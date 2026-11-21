package com.kintmin.domain.playlist

import com.kintmin.domain.testPlaylist
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchDownloadTargetPlaylistsFlowUseCase
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FetchDownloadTargetPlaylistsFlowUseCaseTest {

    private val useCase = FetchDownloadTargetPlaylistsFlowUseCase(mockk(), mockk())

    private val total = testPlaylist(1, PlaylistType.TOTAL)
    private val uncategorized = testPlaylist(2, PlaylistType.UNCATEGORIZED)
    private val favorite = testPlaylist(3, PlaylistType.FAVORITE)
    private val user = testPlaylist(4, PlaylistType.USER)
    private val secret = testPlaylist(5, PlaylistType.SECRET)
    private val all = listOf(total, uncategorized, favorite, user, secret)

    @Test
    fun `사용자·비밀 플레이리스트와 미분류만 고를 수 있다`() {
        assertEquals(listOf(uncategorized, user, secret), useCase.resolve(savedId = null, playlists = all).selectable)
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

}

package com.kintmin.domain.playlist

import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import com.kintmin.domain.testPlaylist
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FetchAllPlaylistFlowUseCaseTest {

    private val isSecretModeEnabled = MutableStateFlow(false)
    private val useCase = FetchAllPlaylistFlowUseCase(
        playlistRepository = mockk<PlaylistRepository> {
            every { getAllPlaylistFlow() } returns flowOf(listOf(testPlaylist(1, PlaylistType.TOTAL), testPlaylist(5, PlaylistType.SECRET)))
        },
        fetchSecretModeEnabledFlowUseCase = mockk<FetchSecretModeEnabledFlowUseCase> { every { this@mockk() } returns isSecretModeEnabled },
    )

    @Test
    fun `비밀모드가 아니면 비밀 플레이리스트를 빼고 돌려준다`() = runTest {
        assertEquals(listOf(1), useCase().first().map { it.id })
    }

    @Test
    fun `비밀모드면 비밀 플레이리스트도 돌려준다`() = runTest {
        isSecretModeEnabled.value = true

        assertEquals(listOf(1, 5), useCase().first().map { it.id })
    }
}

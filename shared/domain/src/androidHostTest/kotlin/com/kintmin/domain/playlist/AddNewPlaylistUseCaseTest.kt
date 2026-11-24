package com.kintmin.domain.playlist

import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.playlist.usecase.AddNewPlaylistUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.SentryEvent
import com.kintmin.log.HIDDEN_TEXT
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddNewPlaylistUseCaseTest {

    private val playlistRepository: PlaylistRepository = mockk()
    private val appLog: AppLog = mockk(relaxed = true)
    private val useCase = AddNewPlaylistUseCase(playlistRepository, appLog)

    @Test
    fun `비밀 플레이리스트를 만들면 제목을 가린다`() = runTest {
        coEvery { playlistRepository.addPlaylist("비밀 목록", PlaylistType.SECRET) } returns Result.success(3)

        useCase("비밀 목록", PlaylistType.SECRET)

        verify { appLog.sendSentryEvent(SentryEvent.AddPlaylist(3, HIDDEN_TEXT)) }
    }

    @Test
    fun `일반 플레이리스트를 만들면 비밀모드와 상관없이 제목을 남긴다`() = runTest {
        coEvery { playlistRepository.addPlaylist("목록", PlaylistType.USER) } returns Result.success(4)

        useCase("목록")

        verify { appLog.sendSentryEvent(SentryEvent.AddPlaylist(4, "목록")) }
    }
}

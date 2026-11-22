package com.kintmin.domain.playlist

import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.playlist.usecase.AddNewPlaylistUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.SentryEvent
import com.kintmin.log.secret
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
    fun `만들면 제목을 비밀로 감싸 남긴다`() = runTest {
        coEvery { playlistRepository.addPlaylist("비밀 목록", PlaylistType.SECRET) } returns Result.success(3)

        useCase("비밀 목록", PlaylistType.SECRET)

        verify { appLog.sendSentryEvent(SentryEvent.AddPlaylist(3, secret("비밀 목록"))) }
    }
}

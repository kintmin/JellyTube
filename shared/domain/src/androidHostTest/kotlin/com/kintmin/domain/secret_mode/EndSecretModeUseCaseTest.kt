package com.kintmin.domain.secret_mode

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import com.kintmin.domain.testPlaylist
import com.kintmin.log.AppLog
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EndSecretModeUseCaseTest {

    private val secretModeRepository: SecretModeRepository = mockk {
        coEvery { setEnabled(any()) } just Runs
        every { isEnabledFlow } returns MutableStateFlow(true)
    }
    private val appSettingRepository: AppSettingRepository = mockk {
        coEvery { updatePlaylistIdOnDownload(any()) } returns Result.success(Unit)
    }
    private val playlistRepository: PlaylistRepository = mockk {
        every { getAllPlaylistFlow() } returns flowOf(
            listOf(testPlaylist(2, PlaylistType.UNCATEGORIZED), testPlaylist(4, PlaylistType.USER), testPlaylist(5, PlaylistType.SECRET)),
        )
    }
    private val appLog: AppLog = mockk(relaxed = true)
    private val useCase = EndSecretModeUseCase(secretModeRepository, appSettingRepository, playlistRepository, appLog)

    @Test
    fun `다운로드 대상이 비밀 플레이리스트면 미분류로 되돌린다`() = runTest {
        every { appSettingRepository.getPlaylistIdOnDownloadFlow() } returns flowOf(5)

        useCase()

        coVerify { secretModeRepository.setEnabled(false) }
        coVerify { appSettingRepository.updatePlaylistIdOnDownload(2) }
    }

    @Test
    fun `다운로드 대상이 비밀 플레이리스트가 아니면 그대로 둔다`() = runTest {
        listOf(4, null).forEach { savedId ->
            every { appSettingRepository.getPlaylistIdOnDownloadFlow() } returns flowOf(savedId)

            useCase()
        }

        coVerify(exactly = 2) { secretModeRepository.setEnabled(false) }
        coVerify(exactly = 0) { appSettingRepository.updatePlaylistIdOnDownload(any()) }
    }

    @Test
    fun `이미 꺼져 있었으면 끔 로그를 남기지 않는다`() = runTest {
        every { secretModeRepository.isEnabledFlow } returns MutableStateFlow(false)
        every { appSettingRepository.getPlaylistIdOnDownloadFlow() } returns flowOf(4)

        useCase()

        verify(exactly = 0) { appLog.sendDebugLog(any()) }
    }

    @Test
    fun `미분류로 되돌리지 못하면 로그를 남긴다`() = runTest {
        every { appSettingRepository.getPlaylistIdOnDownloadFlow() } returns flowOf(5)
        coEvery { appSettingRepository.updatePlaylistIdOnDownload(any()) } returns Result.failure(Exception("boom"))

        useCase()

        verify { appLog.sendDebugLog(match { it.message.startsWith("다운로드 대상을 미분류로 되돌리지 못함") }) }
    }
}

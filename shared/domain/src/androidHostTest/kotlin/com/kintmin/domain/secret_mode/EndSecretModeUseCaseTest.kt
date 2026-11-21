package com.kintmin.domain.secret_mode

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Test
import kotlin.time.Duration

class EndSecretModeUseCaseTest {

    private val secretModeRepository: SecretModeRepository = mockk {
        every { setEnabled(any()) } just Runs
    }
    private val appSettingRepository: AppSettingRepository = mockk {
        coEvery { updatePlaylistIdOnDownload(any()) } returns Result.success(Unit)
    }
    private val playlistRepository: PlaylistRepository = mockk {
        every { getAllPlaylistFlow() } returns flowOf(
            listOf(playlist(2, PlaylistType.UNCATEGORIZED), playlist(4, PlaylistType.USER), playlist(5, PlaylistType.SECRET)),
        )
    }
    private val useCase = EndSecretModeUseCase(secretModeRepository, appSettingRepository, playlistRepository)

    @Test
    fun `다운로드 대상이 비밀 플레이리스트면 미분류로 되돌린다`() = runTest {
        every { appSettingRepository.getPlaylistIdOnDownloadFlow() } returns flowOf(5)

        useCase()

        verify { secretModeRepository.setEnabled(false) }
        coVerify { appSettingRepository.updatePlaylistIdOnDownload(2) }
    }

    @Test
    fun `다운로드 대상이 비밀 플레이리스트가 아니면 그대로 둔다`() = runTest {
        listOf(4, null).forEach { savedId ->
            every { appSettingRepository.getPlaylistIdOnDownloadFlow() } returns flowOf(savedId)

            useCase()
        }

        verify(exactly = 2) { secretModeRepository.setEnabled(false) }
        coVerify(exactly = 0) { appSettingRepository.updatePlaylistIdOnDownload(any()) }
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

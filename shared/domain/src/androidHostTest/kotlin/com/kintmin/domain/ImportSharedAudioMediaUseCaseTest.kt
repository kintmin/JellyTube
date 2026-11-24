package com.kintmin.domain

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AddedAudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import com.kintmin.log.HIDDEN_TEXT
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ImportSharedAudioMediaUseCaseTest {

    private val audioMediaRepository: AudioMediaRepository = mockk()
    private val playlistRepository: PlaylistRepository = mockk()
    private val appLog: AppLog = mockk(relaxed = true)
    private val useCase = ImportSharedAudioMediaUseCase(
        audioMediaRepository = audioMediaRepository,
        fetchShouldInsertAtTopOnDownloadFlowUseCase = mockk { every { this@mockk() } returns flowOf(false) },
        fetchPlaylistIdOnDownloadFlowUseCase = mockk { every { this@mockk() } returns flowOf(null) },
        playlistRepository = playlistRepository,
        appLog = appLog,
    )

    @Test
    fun `들여오면 곡 id 를 남긴다`() = runTest {
        val result: AddedAudioMedia = mockk { every { audioMedia.id } returns 7; every { audioMedia.name } returns "song" }
        coEvery { audioMediaRepository.importSharedAudio(any(), any(), any()) } returns Result.success(result)

        useCase("content://a")

        verify { appLog.sendDebugLog(match { it.tag == "ImportSharedAudio" && it.message == "들여옴: id=7 name=song" }) }
    }

    @Test
    fun `실패하면 주소와 원인을 남긴다`() = runTest {
        coEvery { audioMediaRepository.importSharedAudio(any(), any(), any()) } returns Result.failure(Exception("boom"))

        useCase("content://a")

        verify { appLog.sendErrorLog(match { it.message == "들여오기 실패: content://a" && it.throwable?.message == "boom" }) }
    }

    @Test
    fun `비밀 플레이리스트로 들여오면 이름과 주소를 가린다`() = runTest {
        coEvery { playlistRepository.getPlaylist(9) } returns Result.success(mockk { every { type } returns PlaylistType.SECRET })
        val result: AddedAudioMedia = mockk { every { audioMedia.id } returns 7; every { audioMedia.name } returns "song" }
        coEvery { audioMediaRepository.importSharedAudio(any(), any(), any()) } returns Result.success(result)

        useCase("content://a", playlistIdOverride = 9)

        verify { appLog.sendDebugLog(match { it.message == "들여옴: id=7 name=$HIDDEN_TEXT" }) }
    }
}

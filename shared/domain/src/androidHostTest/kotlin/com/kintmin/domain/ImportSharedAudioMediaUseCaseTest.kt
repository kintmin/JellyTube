package com.kintmin.domain

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AddedAudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.log.AppLog
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ImportSharedAudioMediaUseCaseTest {

    private val audioMediaRepository: AudioMediaRepository = mockk()
    private val appLog: AppLog = mockk(relaxed = true)
    private val useCase = ImportSharedAudioMediaUseCase(
        audioMediaRepository = audioMediaRepository,
        fetchShouldInsertAtTopOnDownloadFlowUseCase = mockk { every { this@mockk() } returns flowOf(false) },
        fetchPlaylistIdOnDownloadFlowUseCase = mockk { every { this@mockk() } returns flowOf(null) },
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

        verify { appLog.sendDebugLog(match { it.message == "들여오기 실패: content://a / java.lang.Exception: boom" }) }
    }
}

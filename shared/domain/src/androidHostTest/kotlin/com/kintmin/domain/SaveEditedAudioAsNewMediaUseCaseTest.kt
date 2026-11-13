package com.kintmin.domain

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_edit.usecase.BlankEditedAudioName
import com.kintmin.domain.audio_edit.usecase.SaveEditedAudioAsNewMediaUseCase
import com.kintmin.log.AppLog
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class SaveEditedAudioAsNewMediaUseCaseTest {

    private companion object {

        const val AUDIO_MEDIA_ID = 1
    }

    private val audioEditRepository: AudioEditRepository = mockk()
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase = mockk()
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase = mockk()
    private val appLog: AppLog = mockk(relaxed = true)

    private val useCase = SaveEditedAudioAsNewMediaUseCase(
        audioEditRepository = audioEditRepository,
        fetchShouldInsertAtTopOnDownloadFlowUseCase = fetchShouldInsertAtTopOnDownloadFlowUseCase,
        fetchPlaylistIdOnDownloadFlowUseCase = fetchPlaylistIdOnDownloadFlowUseCase,
        appLog = appLog,
    )

    private val parts = listOf(AudioEditPart.Audio(startMs = 0, endMs = 1_000))

    @Before
    fun setup() {
        every { audioEditRepository.reserveSaving(any()) } returns true
        every { audioEditRepository.releaseSaving(any()) } just Runs
    }

    @Test
    fun `이름이 비어 실패해도 저장 중 표시를 푼다`() = runTest {
        val result = useCase(audioMediaId = AUDIO_MEDIA_ID, parts = parts, name = " ")

        assertTrue(result.exceptionOrNull() is BlankEditedAudioName)
        verify(exactly = 1) { audioEditRepository.releaseSaving(AUDIO_MEDIA_ID) }
        coVerify(inverse = true) { audioEditRepository.saveEditedAudioAsNewMedia(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `설정을 읽다 예외가 나도 저장 중 표시를 푼다`() = runTest {
        every { fetchPlaylistIdOnDownloadFlowUseCase() } throws IllegalStateException("설정 읽기 실패")

        try {
            useCase(audioMediaId = AUDIO_MEDIA_ID, parts = parts, name = "편집본")
            fail("예외가 전달되어야 한다")
        } catch (e: IllegalStateException) {
            verify(exactly = 1) { audioEditRepository.releaseSaving(AUDIO_MEDIA_ID) }
        }
    }
}

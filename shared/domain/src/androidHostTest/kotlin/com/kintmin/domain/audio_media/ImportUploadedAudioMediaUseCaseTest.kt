package com.kintmin.domain.audio_media

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AddedAudioMedia
import com.kintmin.domain.audio_media.model.UploadedAudioFile
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportUploadedAudioMediaUseCaseTest {

    private val insertedNames = mutableListOf<String>()
    private val audioMediaRepository = mockk<AudioMediaRepository> {
        coEvery { importUploadedAudio(any(), any(), any(), any()) } answers {
            val name = secondArg<String>()
            insertedNames += name
            if (name == FAILING) {
                Result.failure(IllegalStateException(name))
            } else {
                Result.success(mockk<AddedAudioMedia>(relaxed = true))
            }
        }
    }

    private fun useCase(shouldInsertAtTop: Boolean) = ImportUploadedAudioMediaUseCase(
        audioMediaRepository = audioMediaRepository,
        fetchShouldInsertAtTopOnDownloadFlowUseCase = mockk<FetchShouldInsertAtTopOnDownloadFlowUseCase> {
            every { this@mockk.invoke() } returns flowOf(shouldInsertAtTop)
        },
        fetchPlaylistIdOnDownloadFlowUseCase = mockk<FetchPlaylistIdOnDownloadFlowUseCase> {
            every { this@mockk.invoke() } returns flowOf(null)
        },
    )

    private val uploads = listOf("a.mp3", FAILING, "c.mp3").map { UploadedAudioFile("/tmp/$it", it) }

    @Test
    fun `맨 아래에 넣을 때는 요청 순서대로 넣는다`() = runTest {
        useCase(shouldInsertAtTop = false)(uploads)

        assertEquals(listOf("a.mp3", FAILING, "c.mp3"), insertedNames)
    }

    @Test
    fun `맨 위에 넣을 때는 뒤에서부터 넣어 묶음이 요청 순서로 놓이게 한다`() = runTest {
        useCase(shouldInsertAtTop = true)(uploads)

        assertEquals(listOf("c.mp3", FAILING, "a.mp3"), insertedNames)
    }

    @Test
    fun `결과는 요청 순서이고 한 곡의 실패가 뒤 곡을 막지 않는다`() = runTest {
        val results = useCase(shouldInsertAtTop = true)(uploads)

        assertEquals(listOf(true, false, true), results.map { it.isSuccess })
        assertEquals(FAILING, results[1].exceptionOrNull()?.message)
    }

    private companion object {
        const val FAILING = "b.mp3"
    }
}

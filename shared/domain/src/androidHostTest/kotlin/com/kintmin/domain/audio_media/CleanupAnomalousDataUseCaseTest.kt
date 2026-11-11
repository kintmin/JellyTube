package com.kintmin.domain.audio_media

import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CleanupAnomalousDataUseCaseTest {

    private val audioMediaRepository = mockk<AudioMediaRepository> {
        coEvery { deleteOrphanAudioMedia() } returns Result.success(emptyList())
        coEvery { correctAudioFileExtensions() } returns Result.success(2)
        coEvery { listAudioAndImageFileFullPaths() } returns Result.success(emptyList())
        every { getAudioMediaListFlow() } returns flowOf(emptyList())
    }
    private val playlistRepository = mockk<PlaylistRepository> {
        every { getAllPlaylistFlow() } returns flowOf(emptyList())
    }
    private val audioEditRepository = mockk<AudioEditRepository>()
    private val appLog = mockk<AppLog>(relaxed = true)

    private fun useCase() = CleanupAnomalousDataUseCase(audioMediaRepository, playlistRepository, audioEditRepository, appLog)

    @Test
    fun `편집본을 저장하는 중이면 렌더러가 연 파일의 이름을 바꾸지 않는다`() = runTest {
        every { audioEditRepository.observeSavingAudioMediaIds() } returns flowOf(setOf(7))

        useCase()().getOrThrow()

        coVerify(exactly = 0) { audioMediaRepository.correctAudioFileExtensions() }
    }

    @Test
    fun `저장 중인 편집본이 없으면 확장자를 바로잡고 센다`() = runTest {
        every { audioEditRepository.observeSavingAudioMediaIds() } returns flowOf(emptySet())

        assertEquals(2, useCase()().getOrThrow())
    }
}

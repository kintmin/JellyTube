package com.kintmin.domain.audio_media

import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_media.model.AudioMedia
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
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class CleanupAnomalousDataUseCaseTest {

    private val audioMediaRepository = mockk<AudioMediaRepository> {
        coEvery { deleteOrphanAudioMedia() } returns Result.success(emptyList())
        coEvery { correctAudioFileExtensions() } returns Result.success(2)
        coEvery { listAudioAndImageFileFullPaths() } returns Result.success(emptyList())
        coEvery { deleteFileAtFullPath(any()) } returns Result.success(Unit)
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

    @Test
    fun `썸네일을 바꾼 곡은 정본과 고른 파일을 둘 다 남긴다`() = runTest {
        every { audioEditRepository.observeSavingAudioMediaIds() } returns flowOf(emptySet())
        every { audioMediaRepository.getAudioMediaListFlow() } returns flowOf(listOf(thumbnailChangedMedia))
        coEvery { audioMediaRepository.listAudioAndImageFileFullPaths() } returns
            Result.success(listOf("/audio.m4a", "/origin.webp", "/custom.webp", "/주인없는.webp"))

        useCase()().getOrThrow()

        // 정본은 되돌리기 위해 남겨 두는 것이라 화면이 쓰지 않아도 지우지 않는다.
        coVerify(exactly = 0) { audioMediaRepository.deleteFileAtFullPath("/origin.webp") }
        coVerify(exactly = 0) { audioMediaRepository.deleteFileAtFullPath("/custom.webp") }
        coVerify { audioMediaRepository.deleteFileAtFullPath("/주인없는.webp") }
    }

    @Test
    fun `연결이 끊긴 곡은 정본과 고른 썸네일을 함께 지운다`() = runTest {
        every { audioEditRepository.observeSavingAudioMediaIds() } returns flowOf(emptySet())
        coEvery { audioMediaRepository.deleteOrphanAudioMedia() } returns Result.success(listOf(thumbnailChangedMedia))

        useCase()().getOrThrow()

        coVerify { audioMediaRepository.deleteFileAtFullPath("/origin.webp") }
        coVerify { audioMediaRepository.deleteFileAtFullPath("/custom.webp") }
    }
}

private val thumbnailChangedMedia = AudioMedia(
    id = 1,
    source = "source",
    name = "name",
    artist = "artist",
    description = "",
    audioDuration = null,
    audioFileFullPath = "/audio.m4a",
    imageFileFullPath = "/origin.webp",
    customImageFileFullPath = "/custom.webp",
    createdTime = LocalDateTime(2026, 1, 1, 0, 0),
)

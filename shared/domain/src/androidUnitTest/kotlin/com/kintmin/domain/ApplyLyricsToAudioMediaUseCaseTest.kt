package com.kintmin.domain

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.lyrics.usecase.ApplyLyricsToAudioMediaUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplyLyricsToAudioMediaUseCaseTest {

    private val audioMediaRepository = mockk<AudioMediaRepository>()
    private val useCase = ApplyLyricsToAudioMediaUseCase(audioMediaRepository)

    @Test
    fun `synced 가사가 있으면 syncedLyrics를 싱크로 저장하고 컬럼을 갱신한다`() = runTest {
        val path = "/lyrics/abc.lrc"
        coEvery { audioMediaRepository.saveLyrics(any(), any()) } returns Result.success(path)
        coEvery {
            audioMediaRepository.updateAudioMedia(any(), any(), any(), any(), any(), any())
        } returns Result.success(Unit)

        val result = useCase(audioMediaId = 1, plainLyrics = "plain", syncedLyrics = "[00:01.00] hi")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { audioMediaRepository.saveLyrics("[00:01.00] hi", true) }
        coVerify(exactly = 1) {
            audioMediaRepository.updateAudioMedia(id = 1, lyricFileFullPath = path)
        }
    }

    @Test
    fun `synced 가사가 없으면 plainLyrics 각 줄에 태그를 붙여 SYNC로 저장한다`() = runTest {
        val path = "/lyrics/abc.lrc"
        coEvery { audioMediaRepository.saveLyrics(any(), any()) } returns Result.success(path)
        coEvery {
            audioMediaRepository.updateAudioMedia(any(), any(), any(), any(), any(), any())
        } returns Result.success(Unit)

        val result = useCase(audioMediaId = 2, plainLyrics = "첫 줄\n둘째 줄", syncedLyrics = null)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            audioMediaRepository.saveLyrics("[00:00.00]첫 줄\n[00:00.00]둘째 줄", true)
        }
    }

    @Test
    fun `적용할 가사가 전혀 없으면 실패한다`() = runTest {
        val result = useCase(audioMediaId = 3, plainLyrics = null, syncedLyrics = null)
        assertTrue(result.isFailure)
    }
}

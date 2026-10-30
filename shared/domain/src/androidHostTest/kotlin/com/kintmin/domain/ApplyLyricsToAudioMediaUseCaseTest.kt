package com.kintmin.domain

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.lyrics.model.LyricsVariant
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

    @Test
    fun `번역_음차가 있으면 새 원본 파일명으로 각각 저장하고 옛 변형 파일을 정리한다`() = runTest {
        val newPath = "/lyrics/new.lrc"
        val oldPath = "/lyrics/old.lrc"
        coEvery { audioMediaRepository.saveLyrics(any(), any()) } returns Result.success(newPath)
        coEvery { audioMediaRepository.saveVariantLyrics(any(), any(), any()) } returns Result.success(Unit)
        coEvery { audioMediaRepository.deleteVariantLyrics(any(), any()) } returns Result.success(Unit)
        coEvery {
            audioMediaRepository.updateAudioMedia(any(), any(), any(), any(), any(), any())
        } returns Result.success(Unit)

        val result = useCase(
            audioMediaId = 1,
            plainLyrics = null,
            syncedLyrics = "[00:01.00] hi",
            translationLyrics = "[00:01.00] 안녕",
            transliterationLyrics = "[00:01.00] 하이",
            previousLyricFileFullPath = oldPath,
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            audioMediaRepository.saveVariantLyrics(newPath, LyricsVariant.TRANSLATION, "[00:01.00] 안녕")
        }
        coVerify(exactly = 1) {
            audioMediaRepository.saveVariantLyrics(newPath, LyricsVariant.TRANSLITERATION, "[00:01.00] 하이")
        }
        coVerify(exactly = 1) { audioMediaRepository.deleteVariantLyrics(oldPath, LyricsVariant.TRANSLATION) }
        coVerify(exactly = 1) { audioMediaRepository.deleteVariantLyrics(oldPath, LyricsVariant.TRANSLITERATION) }
    }

    @Test
    fun `번역_음차가 없으면 변형 파일을 저장하지 않는다`() = runTest {
        val path = "/lyrics/abc.lrc"
        coEvery { audioMediaRepository.saveLyrics(any(), any()) } returns Result.success(path)
        coEvery {
            audioMediaRepository.updateAudioMedia(any(), any(), any(), any(), any(), any())
        } returns Result.success(Unit)

        val result = useCase(audioMediaId = 1, plainLyrics = null, syncedLyrics = "[00:01.00] hi")

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { audioMediaRepository.saveVariantLyrics(any(), any(), any()) }
    }
}

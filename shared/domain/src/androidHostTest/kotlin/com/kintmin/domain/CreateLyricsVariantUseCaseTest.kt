package com.kintmin.domain

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.repository.LyricsTranslationRepository
import com.kintmin.domain.lyrics.usecase.CreateLyricsVariantUseCase
import com.kintmin.domain.lyrics.usecase.ParseLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SerializeLyricsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateLyricsVariantUseCaseTest {

    private val audioMediaRepository = mockk<AudioMediaRepository>()
    private val translationRepository = mockk<LyricsTranslationRepository>()
    private val useCase = CreateLyricsVariantUseCase(
        audioMediaRepository = audioMediaRepository,
        lyricsTranslationRepository = translationRepository,
        parseLyricsUseCase = ParseLyricsUseCase(),
        serializeLyricsUseCase = SerializeLyricsUseCase(),
    )

    @Test
    fun `싱크 가사를 번역하면 원본 타임스탬프를 유지한 LRC로 저장한다`() = runTest {
        val path = "/lyrics/abc.lrc"
        coEvery { audioMediaRepository.getLyrics(path) } returns
            Result.success("[00:01.00]hello\n[00:03.00]world")
        // 번역 엔진은 줄 구조를 보존해 반환한다고 가정한다.
        coEvery { translationRepository.translate(any(), "en") } returns
            Result.success("안녕\n세계")
        val savedText = slot<String>()
        coEvery {
            audioMediaRepository.saveVariantLyrics(path, LyricsVariant.TRANSLATION, capture(savedText))
        } returns Result.success(Unit)

        val result = useCase(path, LyricsVariant.TRANSLATION, "en")

        assertTrue(result.isSuccess)
        assertEquals("[00:01.00]안녕\n[00:03.00]세계", savedText.captured)
        // 엔진에는 타임태그를 뗀 텍스트만 넘긴다.
        coVerify(exactly = 1) { translationRepository.translate("hello\nworld", "en") }
    }

    @Test
    fun `음차 변환은 transliterate 엔진을 호출한다`() = runTest {
        val path = "/lyrics/xyz.lrc"
        coEvery { audioMediaRepository.getLyrics(path) } returns Result.success("[00:00.50]君")
        coEvery { translationRepository.transliterate(any()) } returns Result.success("키미")
        coEvery {
            audioMediaRepository.saveVariantLyrics(path, LyricsVariant.TRANSLITERATION, any())
        } returns Result.success(Unit)

        val result = useCase(path, LyricsVariant.TRANSLITERATION, "ja")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { translationRepository.transliterate("君") }
    }

    @Test
    fun `가사 내용이 비어 있으면 실패한다`() = runTest {
        val path = "/lyrics/empty.lrc"
        coEvery { audioMediaRepository.getLyrics(path) } returns Result.success("")

        val result = useCase(path, LyricsVariant.TRANSLATION, "en")

        assertTrue(result.isFailure)
    }
}

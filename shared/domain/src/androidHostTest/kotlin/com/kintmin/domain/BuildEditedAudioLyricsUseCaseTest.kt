package com.kintmin.domain

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.BuildEditedAudioLyricsUseCase
import com.kintmin.domain.lyrics.usecase.GetLyricsVariantUseCase
import com.kintmin.domain.lyrics.usecase.GetParsedLyricsUseCase
import com.kintmin.domain.lyrics.usecase.RemapLyricsToAudioEditUseCase
import com.kintmin.domain.lyrics.usecase.SerializeLyricsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildEditedAudioLyricsUseCaseTest {

    private companion object {

        const val LYRIC_PATH = "/lyrics/origin.lrc"
    }

    private val getParsedLyrics = mockk<GetParsedLyricsUseCase>()
    private val getLyricsVariant = mockk<GetLyricsVariantUseCase>()

    private val useCase = BuildEditedAudioLyricsUseCase(
        getParsedLyricsUseCase = getParsedLyrics,
        getLyricsVariantUseCase = getLyricsVariant,
        remapLyricsToAudioEditUseCase = RemapLyricsToAudioEditUseCase(),
        serializeLyricsUseCase = SerializeLyricsUseCase(),
    )

    private fun givenNoVariants() {
        coEvery { getLyricsVariant(any(), any()) } returns Result.success(null)
    }

    @Test
    fun `싱크 가사를 조각 배치에 맞춰 옮긴다`() = runTest {
        coEvery { getParsedLyrics(LYRIC_PATH) } returns listOf(
            LyricsLine(timeMs = 0L, text = "첫 줄"),
            LyricsLine(timeMs = 10_000L, text = "둘째 줄"),
        )
        givenNoVariants()

        val built = useCase(LYRIC_PATH, listOf(AudioEditPart.Audio(startMs = 10_000, endMs = 20_000)))

        assertEquals("[00:00.00]둘째 줄", built?.synced)
        assertNull(built?.translation)
    }

    @Test
    fun `뜻과 읽는 법도 같은 시간축으로 옮긴다`() = runTest {
        coEvery { getParsedLyrics(LYRIC_PATH) } returns listOf(
            LyricsLine(timeMs = 0L, text = "첫 줄"),
            LyricsLine(timeMs = 10_000L, text = "둘째 줄"),
        )
        coEvery { getLyricsVariant(LYRIC_PATH, LyricsVariant.TRANSLATION) } returns Result.success(
            listOf(
                LyricsLine(timeMs = 0L, text = "first"),
                LyricsLine(timeMs = 10_000L, text = "second"),
            )
        )
        coEvery { getLyricsVariant(LYRIC_PATH, LyricsVariant.TRANSLITERATION) } returns Result.success(null)

        val built = useCase(LYRIC_PATH, listOf(AudioEditPart.Audio(startMs = 10_000, endMs = 20_000)))

        assertEquals("[00:00.00]둘째 줄", built?.synced)
        assertEquals("[00:00.00]second", built?.translation)
    }

    @Test
    fun `흐르지 않는 가사는 줄을 그대로 둔다`() = runTest {
        coEvery { getParsedLyrics(LYRIC_PATH) } returns listOf(
            LyricsLine(timeMs = 0L, text = "첫 줄"),
            LyricsLine(timeMs = 0L, text = "둘째 줄"),
        )
        givenNoVariants()

        // 조각이 뒷부분만 떼어 와도 옮길 시간축이 없어 줄이 사라지지 않는다.
        val built = useCase(LYRIC_PATH, listOf(AudioEditPart.Audio(startMs = 10_000, endMs = 20_000)))

        assertEquals("[00:00.00]첫 줄\n[00:00.00]둘째 줄", built?.synced)
    }

    @Test
    fun `원본 가사가 비어 있으면 만들지 않는다`() = runTest {
        coEvery { getParsedLyrics(LYRIC_PATH) } returns emptyList()

        assertNull(useCase(LYRIC_PATH, listOf(AudioEditPart.Audio(startMs = 0, endMs = 1_000))))
    }

    @Test
    fun `소리 조각이 남지 않으면 만들지 않는다`() = runTest {
        coEvery { getParsedLyrics(LYRIC_PATH) } returns listOf(LyricsLine(timeMs = 10_000L, text = "줄"))
        givenNoVariants()

        assertNull(useCase(LYRIC_PATH, listOf(AudioEditPart.Silence(durationMs = 5_000))))
    }
}

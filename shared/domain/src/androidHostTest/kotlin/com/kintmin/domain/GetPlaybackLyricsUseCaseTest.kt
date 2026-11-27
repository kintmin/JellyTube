package com.kintmin.domain

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.GetParsedLyricsUseCase
import com.kintmin.domain.lyrics.usecase.GetPlaybackLyricsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class GetPlaybackLyricsUseCaseTest {

    private companion object {

        const val LYRIC_PATH = "/lyrics/origin.lrc"
    }

    private val getParsedLyrics = mockk<GetParsedLyricsUseCase> {
        coEvery { this@mockk.invoke(LYRIC_PATH) } returns listOf(
            LyricsLine(timeMs = 1_000L, text = "첫 줄"),
            LyricsLine(timeMs = 5_000L, text = "둘째 줄"),
        )
    }

    private val useCase = GetPlaybackLyricsUseCase(getParsedLyrics)

    private val audioMedia = AudioMedia(
        id = 1,
        source = "https://example.com/origin",
        name = "곡",
        artist = "",
        description = "",
        audioDuration = 300.seconds,
        audioFileFullPath = "/audio/origin.m4a",
        imageFileFullPath = null,
        lyricFileFullPath = LYRIC_PATH,
        createdTime = LocalDateTime(2026, 11, 27, 0, 0),
    )

    @Test
    fun `편집본이 없으면 가사의 시각을 그대로 돌려준다`() = runTest {
        val lines = useCase(audioMedia)

        assertEquals(listOf(1_000L, 5_000L), lines.map { it.timeMs })
    }

    @Test
    fun `편집본이 있으면 시각을 떼어 흐르지 않게 한다`() = runTest {
        val edited = audioMedia.copy(
            editedAudioFileFullPath = "/audio/edited.m4a",
            editedAudioDuration = 30.seconds,
        )

        val lines = useCase(edited)

        assertTrue(lines.all { it.timeMs == null })
        // 글은 그대로 보여 준다.
        assertEquals(listOf("첫 줄", "둘째 줄"), lines.map { it.text })
    }

    @Test
    fun `가사가 없으면 빈 목록이다`() = runTest {
        assertTrue(useCase(audioMedia.copy(lyricFileFullPath = null)).isEmpty())
    }
}

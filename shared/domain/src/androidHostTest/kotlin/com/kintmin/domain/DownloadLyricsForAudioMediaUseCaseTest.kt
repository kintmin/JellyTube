package com.kintmin.domain

import com.kintmin.domain.lyrics.model.LyricsSearchResult
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.ApplyLyricsToAudioMediaUseCase
import com.kintmin.domain.lyrics.usecase.BuildLyricsSearchQueryUseCase
import com.kintmin.domain.lyrics.usecase.CreateLyricsVariantUseCase
import com.kintmin.domain.lyrics.usecase.DetectLyricsLanguageUseCase
import com.kintmin.domain.lyrics.usecase.DownloadLyricsForAudioMediaUseCase
import com.kintmin.domain.lyrics.usecase.ParseLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SearchLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SortLyricsSearchResultsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadLyricsForAudioMediaUseCaseTest {

    private val searchLyrics = mockk<SearchLyricsUseCase>()
    private val applyLyrics = mockk<ApplyLyricsToAudioMediaUseCase>()
    private val createVariant = mockk<CreateLyricsVariantUseCase>()

    private val lyricPath = "/lyrics/x.lrc"

    private val useCase = DownloadLyricsForAudioMediaUseCase(
        buildLyricsSearchQuery = BuildLyricsSearchQueryUseCase(),
        searchLyrics = searchLyrics,
        sortLyricsSearchResults = SortLyricsSearchResultsUseCase(),
        applyLyricsToAudioMedia = applyLyrics,
        parseLyrics = ParseLyricsUseCase(),
        detectLyricsLanguage = DetectLyricsLanguageUseCase(),
        createLyricsVariant = createVariant,
    )

    private fun result(
        duration: Double?,
        plain: String?,
        synced: String?,
        trackName: String? = "곡",
    ) = LyricsSearchResult(
        id = 1,
        trackName = trackName,
        artistName = null,
        albumName = null,
        duration = duration,
        plainLyrics = plain,
        syncedLyrics = synced,
    )

    private fun stubApplyAndVariant() {
        coEvery { applyLyrics(any(), any(), any(), any(), any(), any()) } returns Result.success(lyricPath)
        coEvery { createVariant(any(), any(), any()) } returns Result.success(Unit)
    }

    @Test
    fun `길이가 같은 한글 가사면 적용하고 변형은 만들지 않는다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(
            listOf(result(duration = 200.0, plain = "안녕", synced = "[00:01.00] 안녕하세요"))
        )
        stubApplyAndVariant()

        val r = useCase(audioMediaId = 1, title = "곡", targetDurationSeconds = 200.0)

        assertTrue(r.isSuccess)
        coVerify(exactly = 1) { applyLyrics(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { createVariant(any(), any(), any()) }
    }

    @Test
    fun `길이 오차가 1초를 넘으면 적용하지 않는다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(
            listOf(result(duration = 201.5, plain = "hello", synced = null))
        )
        stubApplyAndVariant()

        val r = useCase(audioMediaId = 1, title = "곡", targetDurationSeconds = 200.0)

        assertTrue(r.isSuccess)
        coVerify(exactly = 0) { applyLyrics(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `한글이 없으면 번역 변형을 생성한다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(
            listOf(result(duration = 200.0, plain = "hello world", synced = null))
        )
        stubApplyAndVariant()

        val r = useCase(audioMediaId = 1, title = "곡", targetDurationSeconds = 200.0)

        assertTrue(r.isSuccess)
        coVerify(exactly = 1) { createVariant(lyricPath, LyricsVariant.TRANSLATION, "en") }
        coVerify(exactly = 0) { createVariant(any(), LyricsVariant.TRANSLITERATION, any()) }
    }

    @Test
    fun `일본어면 번역과 음차 변형을 모두 생성한다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(
            listOf(result(duration = 200.0, plain = null, synced = "[00:01.00] こんにちは"))
        )
        stubApplyAndVariant()

        val r = useCase(audioMediaId = 1, title = "곡", targetDurationSeconds = 200.0)

        assertTrue(r.isSuccess)
        coVerify(exactly = 1) { createVariant(lyricPath, LyricsVariant.TRANSLATION, "ja") }
        coVerify(exactly = 1) { createVariant(lyricPath, LyricsVariant.TRANSLITERATION, "ja") }
    }

    @Test
    fun `길이 오차가 1초 이내면 적용한다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(
            listOf(result(duration = 341.438844, plain = "안녕", synced = null))
        )
        stubApplyAndVariant()

        val r = useCase(audioMediaId = 1, title = "곡", targetDurationSeconds = 341.0)

        assertTrue(r.isSuccess)
        coVerify(exactly = 1) { applyLyrics(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `곡명이 제목에 없으면 길이가 같아도 적용하지 않는다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(
            listOf(result(duration = 200.0, plain = "hello", synced = null, trackName = "Daydreaming"))
        )
        stubApplyAndVariant()

        val r = useCase(audioMediaId = 1, title = "Leo", targetDurationSeconds = 200.0)

        assertTrue(r.isSuccess)
        coVerify(exactly = 0) { applyLyrics(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `곡명은 단어 단위로 비교해 다른 단어 속 일부는 일치로 보지 않는다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(
            listOf(result(duration = 200.0, plain = "hello", synced = null, trackName = "Leo"))
        )
        stubApplyAndVariant()

        val r = useCase(audioMediaId = 1, title = "Cleopatra", targetDurationSeconds = 200.0)

        assertTrue(r.isSuccess)
        coVerify(exactly = 0) { applyLyrics(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `제목 앞뒤에 다른 단어가 있어도 곡명이 연속으로 들어 있으면 적용한다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(
            listOf(
                result(
                    duration = 200.0,
                    plain = "hello",
                    synced = null,
                    trackName = "Don't Stop Me Now (Remastered 2011)",
                )
            )
        )
        stubApplyAndVariant()

        val r = useCase(
            audioMediaId = 1,
            title = "AAA BBB - don't stop me now DDD (Official Video)",
            targetDurationSeconds = 200.0,
        )

        assertTrue(r.isSuccess)
        coVerify(exactly = 1) { applyLyrics(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `곡명 순서가 다르면 적용하지 않는다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(
            listOf(result(duration = 200.0, plain = "hello", synced = null, trackName = "Stop Me"))
        )
        stubApplyAndVariant()

        val r = useCase(audioMediaId = 1, title = "Me Stop", targetDurationSeconds = 200.0)

        assertTrue(r.isSuccess)
        coVerify(exactly = 0) { applyLyrics(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `최상위가 곡명이 다르면 조건을 만족하는 다음 결과를 적용한다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(
            listOf(
                result(duration = 200.0, plain = "wrong", synced = null, trackName = "Daydreaming"),
                result(duration = 200.8, plain = "right", synced = null, trackName = "Leo"),
            )
        )
        stubApplyAndVariant()

        val r = useCase(audioMediaId = 1, title = "Leo", targetDurationSeconds = 200.0)

        assertTrue(r.isSuccess)
        coVerify(exactly = 1) { applyLyrics(any(), "right", any(), any(), any(), any()) }
    }

    @Test
    fun `음원 길이를 모르면 적용하지 않는다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(
            listOf(result(duration = 200.0, plain = "안녕", synced = null))
        )
        stubApplyAndVariant()

        val r = useCase(audioMediaId = 1, title = "곡", targetDurationSeconds = null)

        assertTrue(r.isSuccess)
        coVerify(exactly = 0) { applyLyrics(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `검색 결과가 없으면 조용히 성공으로 끝낸다`() = runTest {
        coEvery { searchLyrics(any()) } returns Result.success(emptyList())

        val r = useCase(audioMediaId = 1, title = "곡", targetDurationSeconds = 200.0)

        assertTrue(r.isSuccess)
        coVerify(exactly = 0) { applyLyrics(any(), any(), any(), any(), any(), any()) }
    }
}

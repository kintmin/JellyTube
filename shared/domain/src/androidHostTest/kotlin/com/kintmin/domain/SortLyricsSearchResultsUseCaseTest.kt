package com.kintmin.domain

import com.kintmin.domain.lyrics.model.LyricsSearchResult
import com.kintmin.domain.lyrics.usecase.SortLyricsSearchResultsUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class SortLyricsSearchResultsUseCaseTest {

    private val useCase = SortLyricsSearchResultsUseCase()

    private fun result(id: Int, duration: Double?, synced: Boolean) = LyricsSearchResult(
        id = id,
        trackName = null,
        artistName = null,
        albumName = null,
        duration = duration,
        plainLyrics = "plain",
        syncedLyrics = if (synced) "[00:01.00] hi" else null,
    )

    @Test
    fun `시간 오차가 적은 순으로 정렬한다`() {
        val results = listOf(
            result(1, duration = 210.0, synced = false), // 오차 10
            result(2, duration = 203.0, synced = false), // 오차 3
            result(3, duration = 198.0, synced = false), // 오차 2
        )
        val sorted = useCase(results, targetDurationSeconds = 200.0)
        assertEquals(listOf(3, 2, 1), sorted.map { it.id })
    }

    @Test
    fun `시간 오차가 같으면 SYNC를 우선한다`() {
        val results = listOf(
            result(1, duration = 202.0, synced = false),
            result(2, duration = 202.0, synced = true),
        )
        val sorted = useCase(results, targetDurationSeconds = 200.0)
        assertEquals(listOf(2, 1), sorted.map { it.id })
    }

    @Test
    fun `duration이나 target이 없으면 맨 뒤로 보낸다`() {
        val results = listOf(
            result(1, duration = null, synced = true),
            result(2, duration = 205.0, synced = false),
        )
        val sorted = useCase(results, targetDurationSeconds = 200.0)
        assertEquals(listOf(2, 1), sorted.map { it.id })
    }
}

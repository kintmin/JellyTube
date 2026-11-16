package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsSearchResult
import kotlin.math.abs

/**
 * 가사 검색 결과를 정렬한다.
 * - 1순위: 곡명이 제목에 들어 있는 결과 우선. (LRCLIB 는 아티스트명으로도 걸린 결과를 섞어 준다)
 * - 2순위: 대상 음원과의 재생시간 오차(초)가 적은 순. 시간 정보가 없으면 맨 뒤.
 * - 3순위: SYNC(싱크) 가사 우선.
 */
class SortLyricsSearchResultsUseCase(
    private val matchLyricsTrackName: MatchLyricsTrackNameUseCase,
) {

    operator fun invoke(
        results: List<LyricsSearchResult>,
        title: String,
        targetDurationSeconds: Double?,
    ): List<LyricsSearchResult> =
        results.sortedWith(
            compareBy(
                { !matchLyricsTrackName(title, it.trackName) },
                { timeError(it.duration, targetDurationSeconds) },
                { it.syncedLyrics.isNullOrBlank() },
            )
        )

    private fun timeError(duration: Double?, target: Double?): Double =
        if (duration != null && target != null) abs(duration - target) else Double.MAX_VALUE
}

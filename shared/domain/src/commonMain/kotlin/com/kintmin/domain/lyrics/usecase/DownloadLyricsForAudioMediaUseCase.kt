package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsVariant
import kotlin.math.abs

/**
 * 음원 다운로드 후 가사를 자동으로 확보한다. (조용한 도메인 로직 — 실패해도 로그/UI 없이 무시)
 *
 * 흐름:
 * 1. 제목으로 검색어를 만들어(괄호 제거 + 3단어) 가사를 검색한다.
 * 2. 정렬(시간 오차 -> SYNC) 후 시간 오차 5초 내 최상위 1건을 고른다. 없으면 조용히 종료한다.
 * 3. 고른 가사를 음원에 적용한다.
 * 4. 적용된 가사에 한글이 없으면 번역 파일을, 일본어면 음차 파일을 바로 생성한다. (각각 실패는 무시)
 */
class DownloadLyricsForAudioMediaUseCase(
    private val buildLyricsSearchQuery: BuildLyricsSearchQueryUseCase,
    private val searchLyrics: SearchLyricsUseCase,
    private val sortLyricsSearchResults: SortLyricsSearchResultsUseCase,
    private val applyLyricsToAudioMedia: ApplyLyricsToAudioMediaUseCase,
    private val parseLyrics: ParseLyricsUseCase,
    private val detectLyricsLanguage: DetectLyricsLanguageUseCase,
    private val createLyricsVariant: CreateLyricsVariantUseCase,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        title: String,
        targetDurationSeconds: Double?,
    ): Result<Unit> = runCatching {
        val query = buildLyricsSearchQuery(title)
        val results = searchLyrics(query).getOrNull().orEmpty()

        // 정렬 기준(시간 오차 -> SYNC)으로 정렬한 뒤 시간 오차 5초 내 최상위 1건.
        val picked = sortLyricsSearchResults(results, targetDurationSeconds)
            .firstOrNull {
                targetDurationSeconds != null && it.duration != null &&
                    abs(it.duration - targetDurationSeconds) <= 5.0
            } ?: return@runCatching

        val lyricFileFullPath = applyLyricsToAudioMedia(
            audioMediaId = audioMediaId,
            plainLyrics = picked.plainLyrics,
            syncedLyrics = picked.syncedLyrics,
        ).getOrThrow()

        // 선택한 가사 기준으로 언어를 판별해 조건별 변형을 생성한다.
        val sourceLyrics = picked.syncedLyrics ?: picked.plainLyrics.orEmpty()
        val language = detectLyricsLanguage(parseLyrics(sourceLyrics))

        if (!language.hasKorean) {
            createLyricsVariant(lyricFileFullPath, LyricsVariant.TRANSLATION, language.sourceLanguage)
        }
        if (language.hasJapanese) {
            createLyricsVariant(lyricFileFullPath, LyricsVariant.TRANSLITERATION, language.sourceLanguage)
        }
    }
}

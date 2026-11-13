package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsVariant
import kotlin.math.roundToLong

/**
 * 음원 다운로드 후 가사를 자동으로 확보한다. (조용한 도메인 로직 — 실패해도 로그/UI 없이 무시)
 *
 * 흐름:
 * 1. 제목으로 검색어를 만들어 가사를 검색한다.
 * 2. 정렬(시간 오차 -> SYNC) 후 최상위 1건이 초 단위로 길이가 같을 때만 고른다. 아니면 조용히 종료한다.
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

        // LRCLIB duration 은 정수/소수가 섞여 오므로 반올림한 초로 비교한다.
        val targetSeconds = targetDurationSeconds?.roundToLong() ?: return@runCatching
        val picked = sortLyricsSearchResults(results, targetDurationSeconds)
            .firstOrNull()
            ?.takeIf { it.duration?.roundToLong() == targetSeconds }
            ?: return@runCatching

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

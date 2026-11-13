package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsVariant
import kotlin.math.abs

/**
 * 음원 다운로드 후 가사를 자동으로 확보한다. (조용한 도메인 로직 — 실패해도 로그/UI 없이 무시)
 *
 * 흐름:
 * 1. 제목으로 검색어를 만들어 가사를 검색한다.
 * 2. 정렬(시간 오차 -> SYNC) 순으로, 길이 오차 1초 이내이고 곡명이 제목에 들어 있는 첫 건을 고른다. 없으면 조용히 종료한다.
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

        if (targetDurationSeconds == null) return@runCatching
        val titleWords = words(title)
        // 길이만으로는 아티스트명으로 걸린 비슷한 길이의 다른 곡을 거르지 못한다.
        val picked = sortLyricsSearchResults(results, targetDurationSeconds)
            .firstOrNull {
                it.duration != null && abs(it.duration - targetDurationSeconds) <= 1.0 &&
                    titleWords.containsInOrder(words(it.trackName.orEmpty()))
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

    private fun words(text: String): List<String> =
        buildLyricsSearchQuery(text).lowercase().split(' ').filter { it.isNotEmpty() }

    // 단순 문자열 포함은 단어 경계를 무시해 "Leo" 가 "Cleopatra" 에 걸리므로 단어 단위로 연속 일치를 본다.
    private fun List<String>.containsInOrder(sub: List<String>): Boolean =
        sub.isNotEmpty() && windowed(sub.size).any { it == sub }
}

package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.common_usecase.BuildSongSearchQueryUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.domain.lyrics.model.LyricsVariant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

/**
 * 음원 다운로드 후 가사를 자동으로 확보한다. 실패는 화면에 띄우지 않고 로그로만 남긴다.
 *
 * 흐름:
 * 1. 제목으로 검색어를 만들어 가사를 검색한다.
 * 2. 정렬 순으로, 길이 오차 1초 이내이고 곡명이 제목에 들어 있는 첫 건을 고른다. 없으면 조용히 종료한다.
 * 3. 고른 가사를 음원에 적용한다.
 * 4. 적용된 가사에 한글이 없으면 번역 파일을, 일본어면 음차 파일을 바로 생성한다.
 */
class DownloadLyricsForAudioMediaUseCase(
    private val buildSongSearchQuery: BuildSongSearchQueryUseCase,
    private val searchLyrics: SearchLyricsUseCase,
    private val sortLyricsSearchResults: SortLyricsSearchResultsUseCase,
    private val matchLyricsTrackName: MatchLyricsTrackNameUseCase,
    private val applyLyricsToAudioMedia: ApplyLyricsToAudioMediaUseCase,
    private val parseLyrics: ParseLyricsUseCase,
    private val detectLyricsLanguage: DetectLyricsLanguageUseCase,
    private val createLyricsVariant: CreateLyricsVariantUseCase,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        audioMediaId: Int,
        title: String,
        targetDurationSeconds: Double?,
    ): Result<Unit> = runCatching {
        val query = buildSongSearchQuery(title)
        val results = searchLyrics(query).getOrNull().orEmpty()

        if (targetDurationSeconds == null) return@runCatching
        // 길이만으로는 아티스트명으로 걸린 비슷한 길이의 다른 곡을 거르지 못한다.
        val picked = sortLyricsSearchResults(results, title, targetDurationSeconds)
            .firstOrNull {
                it.duration != null && abs(it.duration - targetDurationSeconds) <= 1.0 &&
                    matchLyricsTrackName(title, it.trackName)
            } ?: return@runCatching log("맞는 가사 없음: id=$audioMediaId (검색 ${results.size}건)")

        val lyricFileFullPath = applyLyricsToAudioMedia(
            audioMediaId = audioMediaId,
            plainLyrics = picked.plainLyrics,
            syncedLyrics = picked.syncedLyrics,
        ).getOrThrow()
        log("가사 적용: id=$audioMediaId")

        // 선택한 가사 기준으로 언어를 판별해 조건별 변형을 생성한다.
        val sourceLyrics = picked.syncedLyrics ?: picked.plainLyrics.orEmpty()
        val language = detectLyricsLanguage(parseLyrics(sourceLyrics))

        if (!language.hasKorean) {
            createLyricsVariant(lyricFileFullPath, LyricsVariant.TRANSLATION, language.sourceLanguage)
                .onFailure { log("번역 만들기 실패: id=$audioMediaId $it") }
        }
        if (language.hasJapanese) {
            createLyricsVariant(lyricFileFullPath, LyricsVariant.TRANSLITERATION, language.sourceLanguage)
                .onFailure { log("음차 만들기 실패: id=$audioMediaId $it") }
        }
    }.onFailure {
        if (it !is CancellationException) appLog.sendErrorLog(ErrorLog(LOG_TAG, "가사 자동 적용 실패: id=$audioMediaId", it))
    }

    private fun log(message: String) = appLog.sendDebugLog(DebugLog(LOG_TAG, message))
}

private const val LOG_TAG = "DownloadLyricsForAudioMedia"

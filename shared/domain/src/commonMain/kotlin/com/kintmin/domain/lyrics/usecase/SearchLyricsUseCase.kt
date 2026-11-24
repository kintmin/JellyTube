package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsSearchResult
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.domain.lyrics.repository.LyricsRepository

class SearchLyricsUseCase(
    private val lyricsRepository: LyricsRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(query: String): Result<List<LyricsSearchResult>> =
        lyricsRepository.search(query)
            // 받은 곡마다 불리고(DownloadLyricsForAudioMediaUseCase) 호출 측이 결과를 삼킨다.
            // LRCLIB 이 흔들리면 곡 수만큼 Issue 가 쌓이므로 디버그로 남긴다. 예외 메시지는 검색어가 든 주소를 담아 싣지 않는다.
            .onFailure { appLog.sendDebugLog(DebugLog("SearchLyricsUseCase", "가사 검색 실패: ${it::class.simpleName}")) }
}

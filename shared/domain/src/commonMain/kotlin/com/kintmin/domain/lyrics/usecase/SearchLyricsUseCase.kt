package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsSearchResult
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.domain.lyrics.repository.LyricsRepository

class SearchLyricsUseCase(
    private val lyricsRepository: LyricsRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(query: String): Result<List<LyricsSearchResult>> =
        lyricsRepository.search(query)
            .onFailure { appLog.sendErrorLog(ErrorLog("SearchLyricsUseCase", "가사 검색 실패: $query", it)) }
}

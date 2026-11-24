package com.kintmin.domain.karaoke.usecase

import com.kintmin.domain.karaoke.model.KaraokeSong
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.domain.karaoke.repository.KaraokeRepository

class SearchKaraokeUseCase(
    private val karaokeRepository: KaraokeRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(title: String): Result<List<KaraokeSong>> =
        karaokeRepository.searchBySongTitle(title)
            // 화면 진입과 검색마다 불린다. 끊긴 망에서 Issue 가 쌓이므로 디버그로 남긴다.
            .onFailure { appLog.sendDebugLog(DebugLog("SearchKaraokeUseCase", "노래방 검색 실패: ${it.message ?: it::class.simpleName}")) }
}

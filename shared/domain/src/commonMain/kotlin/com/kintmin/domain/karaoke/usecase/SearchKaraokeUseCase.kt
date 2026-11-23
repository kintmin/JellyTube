package com.kintmin.domain.karaoke.usecase

import com.kintmin.domain.karaoke.model.KaraokeSong
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.secret
import com.kintmin.domain.karaoke.repository.KaraokeRepository

class SearchKaraokeUseCase(
    private val karaokeRepository: KaraokeRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(title: String): Result<List<KaraokeSong>> =
        karaokeRepository.searchBySongTitle(title)
            .onFailure { appLog.sendErrorLog(ErrorLog("SearchKaraokeUseCase", "노래방 검색 실패: ${secret(title)}", it)) }
}

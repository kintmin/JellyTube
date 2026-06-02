package com.kintmin.domain.app_log.usecase

import com.kintmin.domain.app_log.repository.AppLogRepository

class FetchAppLogLineListUseCase constructor(
    private val appLogRepository: AppLogRepository,
) {
    suspend operator fun invoke(date: String): Result<List<String>> {
        return appLogRepository.fetchAppLogLineList(date)
    }
}

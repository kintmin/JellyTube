package com.kintmin.domain.app_log.usecase

import com.kintmin.domain.app_log.repository.AppLogRepository
import javax.inject.Inject

class FetchAppLogLineListUseCase @Inject constructor(
    private val appLogRepository: AppLogRepository,
) {
    suspend operator fun invoke(date: String): Result<List<String>> {
        return appLogRepository.fetchAppLogLineList(date)
    }
}

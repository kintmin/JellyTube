package com.kintmin.domain.app_log.usecase

import com.kintmin.domain.app_log.repository.AppLogRepository
import javax.inject.Inject

class FetchAppLogDateListUseCase @Inject constructor(
    private val appLogRepository: AppLogRepository,
) {
    suspend operator fun invoke(): Result<List<String>> {
        return appLogRepository.fetchAppLogDateList()
    }
}

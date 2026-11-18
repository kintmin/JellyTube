package com.kintmin.domain.app_log.usecase

import com.kintmin.domain.app_log.repository.AppLogRepository

class FetchAppLogFileFullPathsUseCase constructor(
    private val appLogRepository: AppLogRepository,
) {
    suspend operator fun invoke(): Result<List<String>> = appLogRepository.fetchAppLogFileFullPaths()
}

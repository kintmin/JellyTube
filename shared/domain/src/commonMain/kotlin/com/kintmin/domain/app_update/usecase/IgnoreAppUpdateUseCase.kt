package com.kintmin.domain.app_update.usecase

import com.kintmin.domain.app_update.repository.AppUpdateRepository

class IgnoreAppUpdateUseCase(
    private val appUpdateRepository: AppUpdateRepository,
) {
    suspend operator fun invoke(versionName: String): Result<Unit> =
        appUpdateRepository.updateIgnoredVersionName(versionName)
}

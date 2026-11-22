package com.kintmin.domain.app_update.usecase

import com.kintmin.domain.app_update.AppVersionName
import com.kintmin.domain.app_update.model.AppRelease
import com.kintmin.domain.app_update.repository.AppUpdateRepository

class CheckAppUpdateUseCase(
    private val appUpdateRepository: AppUpdateRepository,
) {
    suspend operator fun invoke(currentVersionName: String): Result<AppRelease?> =
        appUpdateRepository.fetchLatestRelease().map { latest ->
            latest?.takeIf { AppVersionName.isNewer(it.versionName, currentVersionName) }
        }
}

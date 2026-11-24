package com.kintmin.domain.app_update.usecase

import com.kintmin.domain.app_update.AppVersionName
import com.kintmin.domain.app_update.model.AppRelease
import com.kintmin.domain.app_update.repository.AppUpdateRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class CheckAppUpdateOncePerDayUseCase(
    private val appUpdateRepository: AppUpdateRepository,
) {
    suspend operator fun invoke(
        currentVersionName: String,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Result<AppRelease?> {
        val lastCheckMillis = appUpdateRepository.getLastCheckMillis()
        if (lastCheckMillis != null && dateOf(lastCheckMillis, timeZone) == dateOf(nowMillis, timeZone)) {
            return Result.success(null)
        }
        val latest = appUpdateRepository.fetchLatestRelease().getOrElse { return Result.failure(it) }
        appUpdateRepository.updateLastCheckMillis(nowMillis)

        if (latest == null || !AppVersionName.isNewer(latest.versionName, currentVersionName)) {
            return Result.success(null)
        }
        if (latest.versionName == appUpdateRepository.getIgnoredVersionName()) return Result.success(null)
        return Result.success(latest)
    }

    private fun dateOf(millis: Long, timeZone: TimeZone) =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date
}

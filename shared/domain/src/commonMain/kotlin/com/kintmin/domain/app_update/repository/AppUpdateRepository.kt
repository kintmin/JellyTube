package com.kintmin.domain.app_update.repository

import com.kintmin.domain.app_update.model.AppRelease

interface AppUpdateRepository {
    /** 배포된 릴리스가 하나도 없으면 null. */
    suspend fun fetchLatestRelease(): Result<AppRelease?>

    suspend fun getLastCheckMillis(): Long?

    suspend fun updateLastCheckMillis(millis: Long): Result<Unit>

    suspend fun getIgnoredVersionName(): String?

    suspend fun updateIgnoredVersionName(versionName: String): Result<Unit>
}

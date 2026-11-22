package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.PreferencesKey
import com.kintmin.data.network.data_source.GitHubReleaseDataSource
import com.kintmin.data.network.model.GitHubReleaseDto
import com.kintmin.domain.app_update.AppVersionName
import com.kintmin.domain.app_update.model.AppRelease
import com.kintmin.domain.app_update.repository.AppUpdateRepository
import kotlinx.coroutines.flow.first

internal class AppUpdateRepositoryImpl(
    private val gitHubReleaseDataSource: GitHubReleaseDataSource,
    private val datastoreUtil: DatastoreUtil,
) : AppUpdateRepository {

    override suspend fun fetchLatestRelease(): Result<AppRelease?> =
        gitHubReleaseDataSource.fetchReleases().map { releases ->
            releases
                .filter { !it.draft && !it.prerelease && it.tagName.startsWith(MOBILE_TAG_PREFIX) }
                .map { it.toDomain() }
                .maxWithOrNull(byVersion)
        }

    override suspend fun getLastCheckMillis(): Long? =
        datastoreUtil.getData(PreferencesKey.AppUpdateLastCheckMillis).first()

    override suspend fun updateLastCheckMillis(millis: Long): Result<Unit> =
        datastoreUtil.updateData(PreferencesKey.AppUpdateLastCheckMillis, millis)

    override suspend fun getIgnoredVersionName(): String? =
        datastoreUtil.getData(PreferencesKey.AppUpdateIgnoredVersionName).first()

    override suspend fun updateIgnoredVersionName(versionName: String): Result<Unit> =
        datastoreUtil.updateData(PreferencesKey.AppUpdateIgnoredVersionName, versionName)
}

// 릴리스 CI 가 붙이는 태그 모양이다.
private const val MOBILE_TAG_PREFIX = "mobile/v"

private val byVersion = Comparator<AppRelease> { a, b ->
    when {
        AppVersionName.isNewer(a.versionName, b.versionName) -> 1
        AppVersionName.isNewer(b.versionName, a.versionName) -> -1
        else -> 0
    }
}

private fun GitHubReleaseDto.toDomain() = AppRelease(
    versionName = tagName.removePrefix(MOBILE_TAG_PREFIX),
    downloadUrl = assets.firstOrNull { it.name.endsWith(".apk") }?.browserDownloadUrl ?: htmlUrl,
)

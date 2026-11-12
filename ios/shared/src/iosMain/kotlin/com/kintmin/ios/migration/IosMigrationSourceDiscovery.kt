package com.kintmin.ios.migration

import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.presentation.port.MigrationSourceDiscovery
import com.kintmin.presentation.port.model.MigrationSource

internal class IosMigrationSourceDiscovery(
    private val bridge: IosMigrationSourceBrowserBridge,
) : MigrationSourceDiscovery {

    override suspend fun discover(): List<MigrationSource> =
        bridge.browse(FileShareConstants.MIGRATION_NSD_SERVICE_TYPE, SETTLE_MILLIS, DISCOVERY_TIMEOUT_MILLIS)
            .toMigrationSources()

    override suspend fun resolve(source: MigrationSource): MigrationEndpoint? =
        bridge.resolve(FileShareConstants.MIGRATION_NSD_SERVICE_TYPE, source.name, RESOLVE_TIMEOUT_MILLIS)
            ?.let { MigrationEndpoint(host = it.host, port = it.port) }
}

// 이름이 주소를 푸는 열쇠라 이름으로 하나만 남긴다. TXT 가 늦게 풀려 버전이 비면 고르게 둔다 —
// 버전이 정말 다르면 manifest 단계가 막는다.
internal fun List<IosBrowsedService>.toMigrationSources(): List<MigrationSource> =
    groupBy { it.name }
        .map { (name, services) ->
            val version = services.firstNotNullOfOrNull { it.txtRecord[FileShareConstants.MIGRATION_TXT_FORMAT_VERSION] }
            MigrationSource(
                name = name,
                isCompatible = version == null || version == FileShareConstants.MIGRATION_FORMAT_VERSION.toString(),
            )
        }
        .sortedBy { it.name }

// 마지막 변화 뒤 이만큼 조용하면 끝낸다. 짧으면 늦게 보인 두 번째 기기를 놓치고 첫 기기에 자동으로 붙는다.
private const val SETTLE_MILLIS = 3_000L
private const val DISCOVERY_TIMEOUT_MILLIS = 10_000L
private const val RESOLVE_TIMEOUT_MILLIS = 10_000L

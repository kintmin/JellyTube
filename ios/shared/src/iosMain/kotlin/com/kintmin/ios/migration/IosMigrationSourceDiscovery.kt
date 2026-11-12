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
            .map { it.toMigrationSource() }

    override suspend fun resolve(source: MigrationSource): MigrationEndpoint? =
        bridge.resolve(FileShareConstants.MIGRATION_NSD_SERVICE_TYPE, source.name, RESOLVE_TIMEOUT_MILLIS)
            ?.let { MigrationEndpoint(host = it.host, port = it.port) }
}

// TXT 가 늦게 풀려 버전이 비면 고르게 둔다. 버전이 정말 다르면 manifest 단계가 막는다.
internal fun IosBrowsedService.toMigrationSource() = MigrationSource(
    name = name,
    isCompatible = txtRecord[FileShareConstants.MIGRATION_TXT_FORMAT_VERSION]
        ?.let { it == FileShareConstants.MIGRATION_FORMAT_VERSION.toString() }
        ?: true,
)

private const val SETTLE_MILLIS = 1_500L
private const val DISCOVERY_TIMEOUT_MILLIS = 10_000L
private const val RESOLVE_TIMEOUT_MILLIS = 10_000L

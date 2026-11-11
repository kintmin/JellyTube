package com.kintmin.ios.migration

import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.presentation.port.MigrationSourceDiscovery

internal class IosMigrationSourceDiscovery(
    private val bridge: IosMigrationSourceBrowserBridge,
) : MigrationSourceDiscovery {

    override suspend fun discover(): MigrationEndpoint? =
        bridge.findService(FileShareConstants.NSD_SERVICE_TYPE, DISCOVERY_TIMEOUT_MILLIS)
            ?.let { MigrationEndpoint(host = it.host, port = it.port) }
}

private const val DISCOVERY_TIMEOUT_MILLIS = 10_000L

package com.kintmin.jellytube.data_migration

import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.presentation.port.MigrationSourceDiscovery
import com.kintmin.presentation.port.model.MigrationSource

/** 바인딩이 비면 Koin 이 터진다. */
internal class UnsupportedMigrationSourceDiscovery : MigrationSourceDiscovery {

    override suspend fun discover(): List<MigrationSource> = emptyList()

    override suspend fun resolve(source: MigrationSource): MigrationEndpoint? = null
}

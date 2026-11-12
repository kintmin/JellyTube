package com.kintmin.platform.data_migration

import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.presentation.port.MigrationSourceDiscovery
import com.kintmin.presentation.port.model.MigrationSource

/** Android 는 보내는 쪽만 한다. 진입점은 `isDataMigrationReceiveSupported` 가 숨긴다. 바인딩이 비면 Koin 이 터진다. */
internal class UnsupportedMigrationSourceDiscovery : MigrationSourceDiscovery {

    override suspend fun discover(): List<MigrationSource> = emptyList()

    override suspend fun resolve(source: MigrationSource): MigrationEndpoint? = null
}

package com.kintmin.domain.data_migration.repository

import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile

interface DataMigrationRepository {

    suspend fun exportData(): Result<MigrationExport>

    fun resolveFilePath(file: MigrationFile): Result<String>
}

package com.kintmin.domain.data_migration.repository

import com.kintmin.domain.data_migration.model.MigrationApplyResult
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationSnapshot

interface DataMigrationRepository {

    // 보내는 기기

    suspend fun exportData(): Result<MigrationExport>

    fun resolveFilePath(file: MigrationFile): Result<String>

    // 받는 기기

    suspend fun fetchSnapshot(): Result<MigrationSnapshot>

    /** 토큰을 돌려준다. */
    suspend fun pair(endpoint: MigrationEndpoint, code: String): Result<String>

    suspend fun fetchRemoteExport(endpoint: MigrationEndpoint, token: String): Result<MigrationExport>

    /** 임시 공간에 받아 둔다. 같은 크기로 이미 받아 둔 파일은 다시 받지 않는다. [onReceived] 는 이번에 받은 바이트 수다. */
    suspend fun stageFile(
        endpoint: MigrationEndpoint,
        token: String,
        entry: MigrationFileEntry,
        onReceived: (Long) -> Unit,
    ): Result<Unit>

    /** 받아 둔 파일을 제자리로 옮기고 DB 에 한 트랜잭션으로 반영한다. DB 반영이 실패하면 옮긴 파일을 지운다. */
    suspend fun applyPlan(plan: MigrationPlan): Result<MigrationApplyResult>

    suspend fun clearStaging(): Result<Unit>
}

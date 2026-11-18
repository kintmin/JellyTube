package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile

/** 받는 기기가 보낸 이전 요청의 처리. 보내는 서버는 전송·인가만 맡고 처리는 여기로 넘긴다. */
interface MigrationSendHandler {
    suspend fun cleanup(): Result<Unit>

    suspend fun export(): Result<MigrationExport>

    fun resolveFilePath(file: MigrationFile): Result<String>
}

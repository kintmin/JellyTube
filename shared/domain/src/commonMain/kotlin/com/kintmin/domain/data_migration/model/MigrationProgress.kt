package com.kintmin.domain.data_migration.model

data class MigrationProgress(
    val stage: MigrationStage,
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    /** [MigrationStage.TRANSFER] 에서만 채운다. */
    val receivedBytes: Long = 0,
    val totalBytes: Long = 0,
)

package com.kintmin.domain.data_migration.model

data class MigrationProgress(
    val receivedBytes: Long,
    val totalBytes: Long,
)

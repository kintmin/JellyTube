package com.kintmin.domain.data_migration.model

data class MigrationProgress(
    val receivedBytes: Long,
    val totalBytes: Long,
    /** 이 기기에서 재생할 수 없는 형식이라 원본 주소에서 다시 받는 곡 수. */
    val redownloadedCount: Int = 0,
    val redownloadTotal: Int = 0,
)

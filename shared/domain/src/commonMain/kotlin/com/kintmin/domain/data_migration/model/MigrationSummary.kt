package com.kintmin.domain.data_migration.model

data class MigrationSummary(
    val newAudioMediaCount: Int,
    val newPlaylistCount: Int,
    /** 재생할 수 없는 형식이라 원본에서 다시 받아 넣은 곡 수. */
    val redownloadedCount: Int,
    val failures: List<MigrationFailure>,
)

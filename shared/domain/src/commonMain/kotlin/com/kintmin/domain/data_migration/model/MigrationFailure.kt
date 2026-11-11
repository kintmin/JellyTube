package com.kintmin.domain.data_migration.model

/** 들여오지 못한 곡. 이 곡은 DB 에 넣지 않는다 — 음원 없는 행이 남지 않는다. */
data class MigrationFailure(
    val audioMediaName: String,
    val reason: MigrationFailureReason,
)

package com.kintmin.domain.data_migration.model

data class MigrationFailure(
    val audioMediaName: String,
    val reason: MigrationFailureReason,
)

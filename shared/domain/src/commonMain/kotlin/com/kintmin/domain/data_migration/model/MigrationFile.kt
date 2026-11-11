package com.kintmin.domain.data_migration.model

data class MigrationFile(
    val kind: MigrationFileKind,
    val name: String,
)

package com.kintmin.domain.data_migration.model

/** 한 플레이리스트 맨 뒤에 이 순서대로 붙일 곡들. */
data class MigrationTrackAddition(
    val playlist: MigrationRef,
    val audioMedias: List<MigrationRef>,
)

package com.kintmin.domain.data_migration.model

data class MigrationApplyResult(
    /** 새로 만든 플레이리스트의 보내는 쪽 id → 받는 쪽 id. */
    val newPlaylistIdBySourceId: Map<Int, Int>,
)

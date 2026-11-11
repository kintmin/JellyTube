package com.kintmin.domain.data_migration.model

data class MigrationSummary(
    val newAudioMediaCount: Int,
    val newPlaylistCount: Int,
    /** 재생할 수 없는 형식이라 원본 주소에서 다시 받아 넣은 곡 수. */
    val redownloadedCount: Int = 0,
    /** 재생할 수 없는 형식인데 다시 받을 수도 없어 들여오지 않은 곡 이름. */
    val excludedAudioMediaNames: List<String> = emptyList(),
)

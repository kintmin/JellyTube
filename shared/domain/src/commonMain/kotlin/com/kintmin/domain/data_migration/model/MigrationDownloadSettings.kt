package com.kintmin.domain.data_migration.model

/** 이전하는 설정은 이 둘뿐이다. 나머지 설정은 받는 기기 것을 그대로 둔다. */
data class MigrationDownloadSettings(
    val shouldInsertAtTop: Boolean,
    /** 보내는 기기의 플레이리스트 id. null 이면 미분류다. */
    val playlistId: Int?,
)

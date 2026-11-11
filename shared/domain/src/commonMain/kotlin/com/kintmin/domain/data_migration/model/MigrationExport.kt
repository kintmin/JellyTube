package com.kintmin.domain.data_migration.model

/** 보내는 기기가 내놓는 것. [files] 에는 디스크에 실제로 있는 파일만 들어간다. */
data class MigrationExport(
    val snapshot: MigrationSnapshot,
    val files: List<MigrationFileEntry>,
    /** null 이면 설정을 옮기지 않는다. */
    val downloadSettings: MigrationDownloadSettings?,
)

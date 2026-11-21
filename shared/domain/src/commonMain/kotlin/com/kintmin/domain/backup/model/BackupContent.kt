package com.kintmin.domain.backup.model

import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationSnapshot

/** 음원·이미지 파일은 싣지 않는다. 복원할 때 원본 주소에서 다시 받는다. */
data class BackupContent(
    val snapshot: MigrationSnapshot,
    val downloadSettings: MigrationDownloadSettings?,
    /** 가사 파일 이름 → 원문. 사용자가 고친 가사는 다시 찾아 받을 수 없어 싣는다. */
    val lyrics: Map<String, String>,
    val createdAtMillis: Long,
)

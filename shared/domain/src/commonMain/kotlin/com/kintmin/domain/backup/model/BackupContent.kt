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
    /**
     * 이 백업을 쓴 설치. **쓸 때는 저장소가 채우므로** 비워 둔다 — 파일 이름에도 같은 값이 들어가
     * 한 자리에서 정해야 한다. 담기기 전에 만든 백업은 null 이라 되돌려도 계보를 이을 수 없다.
     */
    val installId: String? = null,
)

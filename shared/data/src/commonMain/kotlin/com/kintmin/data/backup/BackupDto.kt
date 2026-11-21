package com.kintmin.data.backup

import com.kintmin.fileshare.MigrationDownloadSettingsDto
import com.kintmin.fileshare.MigrationSnapshotDto
import kotlinx.serialization.Serializable

/** 기기 간 이전 프로토콜과 버전을 따로 센다. 이전 프로토콜이 바뀌어도 옛 백업은 읽혀야 한다. */
@Serializable
internal data class BackupDto(
    val backupVersion: Int,
    val createdAtMillis: Long,
    val snapshot: MigrationSnapshotDto,
    val downloadSettings: MigrationDownloadSettingsDto? = null,
    val lyrics: Map<String, String> = emptyMap(),
)

@Serializable
internal data class BackupVersionDto(
    val backupVersion: Int,
)

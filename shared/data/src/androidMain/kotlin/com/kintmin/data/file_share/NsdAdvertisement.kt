package com.kintmin.data.file_share

import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.fileshare.FileShareConstants

/**
 * 페어링한 뒤에는 이전 타입도 내린다. 다른 기기가 찾아 코드를 넣어 봐야 거절될 뿐이다.
 * 세션이 열린 동안 PC 업로드는 409 라 PC 수신 타입도 광고하지 않는다.
 */
internal fun advertisedServiceTypes(owners: Set<FileShareServerOwner>, migration: DataMigrationSendStatus): Set<String> = when (migration) {
    is DataMigrationSendStatus.WaitingForPair -> setOf(FileShareConstants.MIGRATION_NSD_SERVICE_TYPE)
    is DataMigrationSendStatus.Paired, is DataMigrationSendStatus.Finished -> emptySet()
    DataMigrationSendStatus.Idle, DataMigrationSendStatus.Locked ->
        if (FileShareServerOwner.PC_FILE_SHARE in owners) setOf(FileShareConstants.NSD_SERVICE_TYPE) else emptySet()
}

/** DNS-SD 인스턴스 이름은 UTF-8 63바이트까지다. 넘으면 등록이 실패하는 기기가 있다. */
internal fun toDnsSdInstanceName(name: String): String {
    var byteCount = 0
    var end = 0
    while (end < name.length) {
        val next = name.offsetByCodePoints(end, 1)
        byteCount += name.substring(end, next).encodeToByteArray().size
        if (byteCount > MAX_INSTANCE_NAME_BYTES) break
        end = next
    }
    return name.substring(0, end)
}

private const val MAX_INSTANCE_NAME_BYTES = 63

internal enum class FileShareServerOwner { PC_FILE_SHARE, DATA_MIGRATION }

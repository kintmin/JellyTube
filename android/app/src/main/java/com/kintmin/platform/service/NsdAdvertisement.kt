package com.kintmin.platform.service

import com.kintmin.fileshare.FileShareConstants
import com.kintmin.platform.service.FileShareForegroundService.Owner

internal fun advertisedServiceTypes(owners: Set<Owner>, isMigrationSessionOpen: Boolean): Set<String> = buildSet {
    if (isMigrationSessionOpen) {
        add(FileShareConstants.MIGRATION_NSD_SERVICE_TYPE)
    } else if (Owner.PC_FILE_SHARE in owners) {
        add(FileShareConstants.NSD_SERVICE_TYPE)
    }
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

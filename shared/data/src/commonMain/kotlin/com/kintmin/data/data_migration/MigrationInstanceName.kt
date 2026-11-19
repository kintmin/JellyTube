package com.kintmin.data.data_migration

/**
 * 받는 기기가 이 이름으로 주소를 풀므로 기기 이름이 같아도 겹치지 않게 식별자 앞부분을 붙인다.
 * 63바이트를 넘으면 기기 이름 쪽을 잘라 식별자가 남게 한다.
 */
internal fun toMigrationInstanceName(deviceName: String, deviceId: String?): String {
    val shortId = deviceId?.take(SHORT_ID_LENGTH).orEmpty()
    if (shortId.isEmpty()) return toDnsSdInstanceName(deviceName)
    val suffix = " ($shortId)"
    return toDnsSdInstanceName(deviceName, MAX_INSTANCE_NAME_BYTES - suffix.encodeToByteArray().size) + suffix
}

/** DNS-SD 인스턴스 이름은 UTF-8 63바이트까지다. 넘으면 등록이 실패하는 기기가 있다. */
internal fun toDnsSdInstanceName(name: String, maxBytes: Int = MAX_INSTANCE_NAME_BYTES): String {
    var byteCount = 0
    var end = 0
    while (end < name.length) {
        val isPair = name[end].isHighSurrogate() && end + 1 < name.length && name[end + 1].isLowSurrogate()
        val next = end + if (isPair) 2 else 1
        byteCount += name.substring(end, next).encodeToByteArray().size
        if (byteCount > maxBytes) break
        end = next
    }
    return name.substring(0, end)
}

private const val SHORT_ID_LENGTH = 6
private const val MAX_INSTANCE_NAME_BYTES = 63

package com.kintmin.data.file_share

import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.fileshare.FileShareConstants
import kotlinx.coroutines.flow.Flow

/** 서버가 떠 있는 동안 [start] 에 준 서비스 타입들을 mDNS 로 광고한다. 구현은 플랫폼 소스셋이다. */
internal interface ServiceAdvertiser {
    fun start(serviceTypes: Flow<Set<String>>)
    fun stop()
}

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

internal enum class FileShareServerOwner { PC_FILE_SHARE, DATA_MIGRATION }

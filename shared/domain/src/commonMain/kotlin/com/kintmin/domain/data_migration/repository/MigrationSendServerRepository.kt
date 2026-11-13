package com.kintmin.domain.data_migration.repository

import com.kintmin.domain.data_migration.MigrationSendHandler
import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import kotlinx.coroutines.flow.StateFlow

/** 다른 기기가 이 기기의 데이터를 받아 가도록 여는 서버. 보내는 쪽 서버가 있는 플랫폼(Android)에만 있다. */
interface MigrationSendServerRepository {
    val status: StateFlow<DataMigrationSendStatus>

    /** 이미 열려 있으면 새 코드로 다시 연다. */
    fun open(handler: MigrationSendHandler)

    fun close()
}

package com.kintmin.presentation.port

import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import kotlinx.coroutines.flow.StateFlow

/** 다른 기기가 이 기기의 데이터를 받아 가도록 서버를 열고 페어링 코드를 낸다. */
interface DataMigrationSendController {

    val status: StateFlow<DataMigrationSendStatus>

    /** 이미 열려 있으면 새 코드로 다시 연다. */
    fun start()

    fun stop()
}

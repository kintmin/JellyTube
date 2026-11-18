package com.kintmin.ios.adapter

import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.presentation.port.DataMigrationSendController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS 가 제공하지 않는 포트의 스텁. ViewModel 이 생성자로 받으므로 **바인딩을 비워 두면**
 * 해당 화면에 들어가는 순간 Koin 이 터진다.
 */

/** 데이터 보내기 미제공. */
internal class IosDataMigrationSendControllerStub : DataMigrationSendController {

    override val status: StateFlow<DataMigrationSendStatus> = MutableStateFlow(DataMigrationSendStatus.Idle)

    override fun start() = Unit
    override fun stop() = Unit
}

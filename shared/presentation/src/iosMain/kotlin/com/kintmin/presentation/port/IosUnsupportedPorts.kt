package com.kintmin.presentation.port

import com.kintmin.presentation.port.model.DataMigrationSendStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// iOS 가 제공하지 않는 포트의 스텁. ViewModel 이 생성자로 받으므로 바인딩을 비워 두면
// 해당 화면에 들어가는 순간 Koin 이 터진다.

/** PC 파일 수신 미제공. 수신 서버와 그 서버를 붙잡아 두는 포그라운드 서비스가 Android 에만 있다. */
internal class IosFileShareServerControllerStub : FileShareServerController {

    override val isRunning: StateFlow<Boolean> = MutableStateFlow(false)

    override fun start() = Unit
    override fun stop() = Unit
}

/** 데이터 보내기 미제공. iOS 는 받는 쪽만 한다 — 보내는 서버가 Android 에만 있다. */
internal class IosDataMigrationSendControllerStub : DataMigrationSendController {

    override val status: StateFlow<DataMigrationSendStatus> = MutableStateFlow(DataMigrationSendStatus.Idle)

    override fun start() = Unit
    override fun stop() = Unit
}

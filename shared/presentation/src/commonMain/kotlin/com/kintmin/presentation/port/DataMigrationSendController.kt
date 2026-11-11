package com.kintmin.presentation.port

import kotlinx.coroutines.flow.StateFlow

/** 다른 기기가 이 기기의 데이터를 받아 가도록 서버를 열고 페어링 코드를 낸다. */
interface DataMigrationSendController {

    /** 열려 있는 세션의 코드. 열지 않았거나 코드를 여러 번 틀려 닫혔으면 null. */
    val pairingCode: StateFlow<String?>

    /** 이미 열려 있으면 새 코드로 다시 연다. */
    fun start()

    fun stop()
}

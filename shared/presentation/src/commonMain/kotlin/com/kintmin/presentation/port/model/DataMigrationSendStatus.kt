package com.kintmin.presentation.port.model

sealed interface DataMigrationSendStatus {
    data object Idle : DataMigrationSendStatus
    data class WaitingForPair(val code: String) : DataMigrationSendStatus

    /** 받는 기기가 연결해 받고 있다. 이 동안 화면을 떠나면 받는 쪽이 실패한다. */
    data class Paired(val peerName: String) : DataMigrationSendStatus
    data class Finished(val newAudioMediaCount: Int, val failedCount: Int) : DataMigrationSendStatus

    /** 코드를 여러 번 틀려 연결을 막았다. */
    data object Locked : DataMigrationSendStatus
}

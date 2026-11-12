package com.kintmin.ios.migration

/** Swift 가 구현하는 Bonjour 탐색 창구. 구현은 `MigrationSourceBrowserBridgeImpl`(NWBrowser) 이다. */
interface IosMigrationSourceBrowserBridge {

    /**
     * [serviceType] 탐색 결과를 그대로 준다. 같은 기기가 네트워크 인터페이스마다 따로 올 수 있다.
     * 첫 결과 뒤 목록이 [settleMillis] 동안 그대로면 끝내고, 아무것도 없으면 [timeoutMillis] 에 끝낸다.
     */
    suspend fun browse(serviceType: String, settleMillis: Long, timeoutMillis: Long): List<IosBrowsedService>

    /** [browse] 가 준 [name] 의 기기를 IPv4 주소와 포트로 푼다. [timeoutMillis] 안에 못 풀면 null. */
    suspend fun resolve(serviceType: String, name: String, timeoutMillis: Long): IosResolvedService?
}

/** 브릿지는 Swift 가 볼 수 없는 `presentation`·`domain` 타입 대신 이 거울 DTO 를 주고받는다. */
data class IosBrowsedService(
    val name: String,
    val txtRecord: Map<String, String>,
)

data class IosResolvedService(
    val host: String,
    val port: Int,
)

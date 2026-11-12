package com.kintmin.ios.migration

/** Swift 가 구현하는 Bonjour 탐색 창구. 구현은 `MigrationSourceBrowserBridgeImpl`(NWBrowser) 이다. */
interface IosMigrationSourceBrowserBridge {

    /**
     * [serviceType] 을 광고하는 기기들. 첫 기기를 찾은 뒤 목록이 [settleMillis] 동안 그대로면 끝낸다.
     * 아무것도 못 찾으면 [timeoutMillis] 에 빈 목록으로 끝낸다.
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

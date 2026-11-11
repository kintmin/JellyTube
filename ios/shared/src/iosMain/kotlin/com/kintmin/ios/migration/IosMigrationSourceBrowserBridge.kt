package com.kintmin.ios.migration

/** Swift 가 구현하는 Bonjour 탐색 창구. 구현은 `MigrationSourceBrowserBridgeImpl`(NWBrowser) 이다. */
interface IosMigrationSourceBrowserBridge {

    /** [serviceType] 을 광고하는 첫 기기를 IPv4 주소와 포트로 푼다. [timeoutMillis] 안에 못 찾으면 null. */
    suspend fun findService(serviceType: String, timeoutMillis: Long): IosResolvedService?
}

/** 브릿지는 Swift 가 볼 수 없는 `presentation`·`domain` 타입 대신 이 거울 DTO 를 주고받는다. */
data class IosResolvedService(
    val host: String,
    val port: Int,
)

package com.kintmin.ios.log

/**
 * 로그 수집기로 나가는 **단 하나의 창구**. Kotlin/Native 가 Firebase iOS SDK 를 cinterop
 * 없이 부를 수 없어 Swift 가 구현한다. 레벨은 수집기마다 체계가 달라 두지 않는다.
 */
interface IosAppLogBridge {

    fun setUserId(userId: String)

    fun debug(tag: String, message: String)

    fun event(name: String, params: Map<String, String>)
}

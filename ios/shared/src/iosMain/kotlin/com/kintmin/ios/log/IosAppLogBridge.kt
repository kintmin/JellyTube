package com.kintmin.ios.log

/**
 * 로그 수집기(Firebase/Sentry/…)로 나가는 **단 하나의 창구**. Swift 가 구현한다.
 *
 * 공유 코드가 수집기 SDK 를 모르게 하려는 것이고, Kotlin/Native 가 Firebase iOS SDK 를
 * cinterop 없이 부를 수 없다는 사정도 겹친다.
 *
 * 받는 모양은 "이름 + 문자열 태그" 하나뿐이다. 레벨은 수집기마다 체계가 달라 두지 않는다.
 */
interface IosAppLogBridge {

    fun setUserId(userId: String)

    fun debug(tag: String, message: String)

    fun event(name: String, params: Map<String, String>)
}

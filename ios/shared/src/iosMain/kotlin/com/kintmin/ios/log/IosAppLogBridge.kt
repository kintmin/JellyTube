package com.kintmin.ios.log

/**
 * 로그 수집기(Firebase/Sentry/…)로 나가는 **단 하나의 창구**. Swift 가 구현한다.
 *
 * 공유 코드는 수집기 SDK 를 모른다 — 그래야 수집기를 바꿀 때 Swift 구현 한 곳만 갈아끼우면
 * 된다. Kotlin/Native 가 Firebase iOS SDK 를 cinterop 없이 부를 수 없다는 사정도 겹친다.
 *
 * 받는 모양은 **"이름 + 문자열 태그"** 하나뿐이다. 레벨을 두지 않은 이유는 수집기마다
 * 레벨 체계가 달라 공유 쪽에서 정하면 어느 수집기에서도 어긋나기 때문이다.
 */
interface IosAppLogBridge {

    fun setUserId(userId: String)

    fun debug(tag: String, message: String)

    fun event(name: String, params: Map<String, String>)
}

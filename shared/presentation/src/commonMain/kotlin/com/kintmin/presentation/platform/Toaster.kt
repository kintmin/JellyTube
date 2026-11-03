package com.kintmin.presentation.platform

/**
 * 짧은 안내 메시지를 띄운다.
 *
 * Android 는 Toast 로, 다른 플랫폼은 각자의 방식으로 구현한다.
 * Application 컨텍스트로 충분하므로 Koin 으로 주입받는다.
 *
 * MVI 의 `Event.ShowToast` 계층은 순수 Kotlin 이라 이 포트와 무관하다.
 * 바뀌는 것은 이벤트를 소비하는 쪽 한 줄뿐이다.
 *
 * TODO: Tier2(공통 컴포넌트 슬롯)에서 공통 Snackbar 로 대체하면 이 포트는 사라진다.
 *       iOS 에는 Toast 대응물이 없어 어차피 Snackbar 로 가야 한다.
 */
interface Toaster {

    fun show(message: String)

    fun showLong(message: String)
}

package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/**
 * 짧은 안내 메시지를 띄운다.
 *
 * MVI 의 `Event.ShowToast` 계층은 순수 Kotlin 이라 이 포트와 무관하다.
 *
 * **Snackbar 로 바꾸지 않는다.** Material3 Snackbar 는 안드로이드 언어라 iOS 에서 이질적이고,
 * 화면마다 `Scaffold` 에 `snackbarHost` 를 달아야 해서 침습적이다. 대신 플랫폼마다 다른
 * 방식으로 띄운다 — Android 는 네이티브 `Toast`, iOS 는 [PlatformToastHost] 가 그리는
 * 오버레이다. 호출부는 어느 쪽인지 알 필요가 없다.
 */
interface Toaster {

    fun show(message: String)

    fun showLong(message: String)
}

/**
 * 플랫폼 구현을 Composition 에서 만든다.
 *
 * Android 구현은 `Context` 가 필요하고 그건 `LocalContext` 로만 얻는다.
 * 분기 방식 선택 기준은 `shared/presentation/AGENTS.md` 의 "플랫폼 탈출구" 절 참고.
 */
@Composable
expect fun rememberToaster(): Toaster

/**
 * iOS 에서 토스트를 그리는 자리. 앱 루트에 **한 번만** 깐다.
 *
 * iOS 에는 시스템 토스트 API 가 없어 직접 그리는 수밖에 없다 — Apple 자사 앱도 그렇게 한다.
 * Android 는 네이티브 `Toast` 가 창 위에 뜨므로 아무것도 하지 않는다.
 *
 * **전체화면 다이얼로그 위에는 올라가지 않는다.** 다이얼로그가 별도 레이어에 그려지기
 * 때문이다. 다이얼로그 안에서 띄우는 토스트가 생기면 그쪽에도 호스트를 하나 더 깔아야 한다.
 */
@Composable
expect fun PlatformToastHost()

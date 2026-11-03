package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/**
 * 짧은 안내 메시지를 띄운다.
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

/**
 * 플랫폼 구현을 Composition 에서 만든다.
 *
 * Android 구현은 `Context` 가 필요하고 그건 `LocalContext` 로만 얻는다.
 * 분기 방식 선택 기준은 `shared/presentation/AGENTS.md` 의 "플랫폼 탈출구" 절 참고.
 */
@Composable
expect fun rememberToaster(): Toaster

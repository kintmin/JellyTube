package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS 는 `UIActivityViewController` 로 공유 시트를 띄워야 한다.
 *
 * ComposeUIViewController 바깥의 UIKit 작업이라 iOS 화면 작업과 함께 다룬다.
 * 그때까지 실패로 답한다 — 호출자가 "로그 전송을 시작하지 못했습니다"를 띄워
 * 사용자가 아무 반응 없는 버튼을 누르는 상황을 피한다.
 */
@Composable
actual fun rememberAppLogSharer(): AppLogSharer =
    remember { AppLogSharer { AppLogShareResult.Failed } }

package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS 권한 모델은 Android 와 다르다.
 *
 * - [AppPermission.MediaStorage]: 앱 샌드박스 안에서만 다루므로 권한이 없다.
 * - [AppPermission.Notification]: `UNUserNotificationCenter` 로 요청해야 한다.
 * - [AppPermission.ActivityRecognition]: `CMMotionActivityManager` 권한이 필요하다.
 *
 * 뒤 둘은 iOS 기능 작업과 함께 다룬다. 그때까지 요청은 no-op 이고
 * 판정은 "필요 없음"으로 답한다 — 그래야 화면이 권한 대기 상태에 갇히지 않는다.
 */
private object IosPermissionController : PermissionController {

    override fun isGranted(permission: AppPermission): Boolean = true

    override fun request(
        permissions: List<AppPermission>,
        onResult: (Map<AppPermission, Boolean>) -> Unit,
    ) = Unit
}

@Composable
actual fun rememberPermissionController(): PermissionController = remember { IosPermissionController }

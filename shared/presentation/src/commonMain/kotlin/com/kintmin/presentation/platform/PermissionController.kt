package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/** 앱이 요구하는 권한. OS 버전별로 어떤 원시 권한이 필요한지는 구현이 판단한다. */
enum class AppPermission {

    Notification,

    /** 기기 저장소의 음원 파일 읽기·쓰기. */
    MediaStorage,

    /** 걸음수 측정을 위한 신체 활동 인식. */
    ActivityRecognition,
}

interface PermissionController {

    /** 이미 허용됐거나 이 플랫폼·OS 버전에서 필요하지 않으면 true. */
    fun isGranted(permission: AppPermission): Boolean

    /**
     * 아직 허용되지 않은 것만 골라 요청한다. 요청할 것이 없으면 아무 일도 하지 않고
     * [onResult] 도 부르지 않는다.
     */
    fun request(
        permissions: List<AppPermission>,
        onResult: (Map<AppPermission, Boolean>) -> Unit = {},
    )
}

@Composable
expect fun rememberPermissionController(): PermissionController

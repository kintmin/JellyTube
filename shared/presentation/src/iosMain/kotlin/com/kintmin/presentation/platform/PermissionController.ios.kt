package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreMotion.CMAuthorizationStatusAuthorized
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * 알림 권한 상태 캐시.
 *
 * [PermissionController.isGranted] 는 동기인데 `UNUserNotificationCenter` 의 조회는
 * 비동기다. 마지막으로 확인한 값을 들고 있다가 돌려주고, 컨트롤러가 붙을 때와 요청 직후에
 * 갱신한다.
 *
 * 캐시가 비어 있으면 "아직 아님" 으로 답해 요청이 한 번 더 나가게 한다 — **이미 결정된
 * 상태라면 시스템이 창을 띄우지 않고 바로 답을 돌려주므로** 사용자에게 보이는 비용이 없다.
 */
private object NotificationAuthorization {

    var isAuthorized: Boolean = false

    fun refresh() {
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                val status = settings?.authorizationStatus
                isAuthorized = status == UNAuthorizationStatusAuthorized ||
                    status == UNAuthorizationStatusProvisional
            }
    }

    fun request(onResult: (Boolean) -> Unit) {
        val options = UNAuthorizationOptionAlert or
            UNAuthorizationOptionSound or
            UNAuthorizationOptionBadge

        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(options) { granted, _ ->
                isAuthorized = granted
                onMainQueue { onResult(granted) }
            }
    }
}

/**
 * 모션 권한.
 *
 * **iOS 에는 모션 권한을 "요청" 하는 API 가 없다.** 실제로 질의를 한 번 던지면 시스템이
 * 창을 띄우고, 사용자가 답한 뒤 완료 핸들러가 돌아온다. 그때 상태를 다시 읽는다.
 *
 * `Info.plist` 에 `NSMotionUsageDescription` 이 없으면 질의하는 순간 앱이 죽는다.
 */
private object MotionAuthorization {

    /** 질의가 끝나기 전에 해제되지 않도록 인스턴스를 들고 있는다. */
    private val pedometer by lazy { CMPedometer() }

    val isAuthorized: Boolean
        get() = CMPedometer.authorizationStatus() == CMAuthorizationStatusAuthorized

    fun request(onResult: (Boolean) -> Unit) {
        if (!CMPedometer.isStepCountingAvailable()) {
            onMainQueue { onResult(false) }
            return
        }

        // 길이가 0 인 구간은 질의로 취급되지 않을 수 있어 1초를 준다.
        val now = NSDate()
        val oneSecondAgo = NSDate(timeIntervalSinceReferenceDate = now.timeIntervalSinceReferenceDate - 1.0)

        pedometer.queryPedometerDataFromDate(oneSecondAgo, toDate = now) { _, _ ->
            onMainQueue { onResult(isAuthorized) }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun onMainQueue(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue()) { block() }
}

private class IosPermissionController : PermissionController {

    override fun isGranted(permission: AppPermission): Boolean = when (permission) {
        // 앱 샌드박스 안에서만 파일을 다룬다. 바깥 파일은 문서 선택기로 사용자가 직접 고른
        // 것만 읽으므로 따로 받을 권한이 없다.
        AppPermission.MediaStorage -> true
        AppPermission.Notification -> NotificationAuthorization.isAuthorized
        AppPermission.ActivityRecognition -> MotionAuthorization.isAuthorized
    }

    override fun request(
        permissions: List<AppPermission>,
        onResult: (Map<AppPermission, Boolean>) -> Unit,
    ) {
        val notGranted = permissions.filterNot { isGranted(it) }
        if (notGranted.isEmpty()) return

        // 요청이 각각 비동기로 돌아오므로 전부 모인 뒤에 한 번만 알린다.
        // 완료 콜백은 메인 큐로 모아 두어 이 집계가 한 스레드에서만 돈다.
        val results = mutableMapOf<AppPermission, Boolean>()

        fun complete(permission: AppPermission, isGranted: Boolean) {
            results[permission] = isGranted
            if (results.size == notGranted.size) onResult(results)
        }

        notGranted.forEach { permission ->
            when (permission) {
                AppPermission.MediaStorage -> complete(permission, true)
                AppPermission.Notification -> NotificationAuthorization.request {
                    complete(permission, it)
                }
                AppPermission.ActivityRecognition -> MotionAuthorization.request {
                    complete(permission, it)
                }
            }
        }
    }
}

@Composable
actual fun rememberPermissionController(): PermissionController {
    val controller = remember { IosPermissionController() }

    // 알림 권한은 동기로 읽을 수 없다. 화면이 붙을 때 캐시를 한 번 맞춘다.
    LaunchedEffect(Unit) { NotificationAuthorization.refresh() }

    return controller
}

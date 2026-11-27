package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreMotion.CMAuthorizationStatusAuthorized
import platform.CoreMotion.CMAuthorizationStatusNotDetermined
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_MSEC
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_time
import platform.darwin.dispatch_get_main_queue

/**
 * [PermissionController.isGranted] 는 동기인데 `UNUserNotificationCenter` 조회는 비동기다.
 * 마지막으로 확인한 값을 돌려주고, 비어 있으면 "아직 아님" 으로 답해 요청을 한 번 더 내보낸다
 * (이미 결정된 상태면 시스템이 창 없이 바로 답한다).
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
 * iOS 에는 모션 권한을 요청하는 API 가 없다. 질의를 한 번 던지면 시스템이 창을 띄운다.
 * `Info.plist` 에 `NSMotionUsageDescription` 이 없으면 그 순간 앱이 죽는다.
 *
 * **질의의 완료 핸들러로 답을 판단하면 안 된다.** 창이 아직 떠 있는 동안 `CMErrorNotAuthorized`
 * 로 먼저 돌아오므로, 그때 읽은 상태는 늘 `notDetermined` 다. 상태가 결정될 때까지 센다.
 */
private object MotionAuthorization {

    /** 질의가 끝나기 전에 해제되지 않도록 인스턴스를 들고 있는다. */
    private val pedometer by lazy { CMPedometer() }

    private const val POLL_INTERVAL_MILLIS = 250L
    private const val POLL_TIMEOUT_MILLIS = 60_000L

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
        pedometer.queryPedometerDataFromDate(oneSecondAgo, toDate = now) { _, _ -> }

        awaitSettledStatus(elapsedMillis = 0L, onResult = onResult)
    }

    private fun awaitSettledStatus(elapsedMillis: Long, onResult: (Boolean) -> Unit) {
        if (CMPedometer.authorizationStatus() != CMAuthorizationStatusNotDetermined) {
            onMainQueue { onResult(isAuthorized) }
            return
        }
        if (elapsedMillis >= POLL_TIMEOUT_MILLIS) {
            onMainQueue { onResult(false) }
            return
        }

        afterOnMainQueue(POLL_INTERVAL_MILLIS) {
            awaitSettledStatus(elapsedMillis + POLL_INTERVAL_MILLIS, onResult)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun onMainQueue(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue()) { block() }
}

@OptIn(ExperimentalForeignApi::class)
private fun afterOnMainQueue(delayMillis: Long, block: () -> Unit) {
    val delay = dispatch_time(DISPATCH_TIME_NOW, delayMillis * NSEC_PER_MSEC.toLong())
    dispatch_after(delay, dispatch_get_main_queue()) { block() }
}

private class IosPermissionController : PermissionController {

    override fun isGranted(permission: AppPermission): Boolean = when (permission) {
        // 샌드박스 밖 파일은 문서 선택기로 고른 것만 읽어서 받을 권한이 없다.
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

        // 각각 비동기로 돌아온다. 메인 큐로 모아 한 스레드에서 집계하고 한 번만 알린다.
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

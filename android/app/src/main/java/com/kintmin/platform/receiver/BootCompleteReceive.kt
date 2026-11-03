package com.kintmin.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kintmin.platform.service.StepForegroundService

/**
 * 참고사항: ACTION_BOOT_COMPLETED 백그라운드에서도 Foreground 실행 가능
 * https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start#background-start-restriction-exemptions
 *
 * 참고사항: Android 15(API 35)부터 앱이 Stopped 상태에서 해제되면 ACTION_BOOT_COMPLETED가 다시 전달됨
 * https://developer.android.com/about/versions/15/behavior-changes-all#enhanced-stop-states
 */
class BootCompleteReceive : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        when(intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                StepForegroundService.startService(context)
            }
        }
    }
}

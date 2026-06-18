package com.kintmin.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kintmin.platform.service.StepForegroundService

/**
 * 참고사항: ACTION_BOOT_COMPLETED 백그라운드에서도 Foreground 실행 가능
 * https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start#background-start-restriction-exemptions
 */
class BootCompleteReceive : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when(intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                StepForegroundService.startService(context, isStartedAfterBoot = true)
            }
        }
    }
}

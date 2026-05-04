package com.kintmin.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kintmin.platform.service.StepForegroundService

class PackageChangedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when(intent?.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                runCatching {
                    StepForegroundService.startService(context)
                }
            }
        }
    }
}
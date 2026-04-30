package com.kintmin.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kintmin.platform.service.StepForegroundService

class BootCompleteReceive : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when(intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                runCatching {
                    ContextCompat.startForegroundService(context, Intent(context, StepForegroundService::class.java))
                }
            }
        }
    }
}
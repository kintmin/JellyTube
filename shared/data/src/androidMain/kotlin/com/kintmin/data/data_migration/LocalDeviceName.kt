package com.kintmin.data.data_migration

import android.content.Context
import android.os.Build
import android.provider.Settings

/** 다른 기기에 보여 줄 이 기기의 이름. 사용자가 설정에서 정한 이름을 먼저 쓴다. */
fun localDeviceName(context: Context): String =
    Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        ?.takeIf { it.isNotBlank() }
        ?: Build.MODEL

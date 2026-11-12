package com.kintmin.data.data_migration

import android.os.Build

internal actual suspend fun localDeviceName(): String = Build.MODEL

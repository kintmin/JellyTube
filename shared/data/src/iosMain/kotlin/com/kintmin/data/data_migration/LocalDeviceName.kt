package com.kintmin.data.data_migration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.UIKit.UIDevice

/** 페어링할 때 보내는 기기 화면에 보여 줄 이 기기의 이름. UIKit 은 메인 스레드에서만 읽는다. */
internal suspend fun localDeviceName(): String = withContext(Dispatchers.Main) { UIDevice.currentDevice.name }

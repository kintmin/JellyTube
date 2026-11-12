package com.kintmin.data.data_migration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.UIKit.UIDevice

// UIKit 은 메인 스레드에서만 읽는다.
internal actual suspend fun localDeviceName(): String = withContext(Dispatchers.Main) { UIDevice.currentDevice.name }

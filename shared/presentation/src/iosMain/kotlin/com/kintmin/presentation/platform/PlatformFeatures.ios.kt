package com.kintmin.presentation.platform

import platform.UIKit.UIDevice

/** 이유는 `ios/app/AGENTS.md` §"iOS 에서 제공하지 않는 것". */
actual val isFileShareReceiveSupported: Boolean = true

actual val isDeviceSaveSupported: Boolean = true

actual val deviceSaveFlow: DeviceSaveFlow =
    if (isRingtoneShareSupported(UIDevice.currentDevice.systemVersion)) {
        DeviceSaveFlow.RingtoneShareSheet
    } else {
        DeviceSaveFlow.RingtoneNeedsNewerOs
    }

/** 보내는 서버가 Android 에만 있다. */
actual val isDataMigrationSendSupported: Boolean = false

actual val isDataMigrationReceiveSupported: Boolean = true

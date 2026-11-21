package com.kintmin.presentation.platform

import platform.UIKit.UIDevice

actual val isFileShareReceiveSupported: Boolean = true

actual val isDeviceSaveSupported: Boolean = true

actual val deviceSaveFlow: DeviceSaveFlow =
    if (isRingtoneShareSupported(UIDevice.currentDevice.systemVersion)) {
        DeviceSaveFlow.RingtoneShareSheet
    } else {
        DeviceSaveFlow.RingtoneNeedsNewerOs
    }

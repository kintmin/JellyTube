package com.kintmin.presentation.platform

actual val isDeviceSaveSupported: Boolean = true

actual val deviceSaveFlow: DeviceSaveFlow = DeviceSaveFlow.MediaStore

package com.kintmin.presentation.platform

actual val isFileShareReceiveSupported: Boolean = true

actual val isDeviceSaveSupported: Boolean = true

actual val deviceSaveFlow: DeviceSaveFlow = DeviceSaveFlow.MediaStore

package com.kintmin.presentation.platform

actual val isAudioEditSupported: Boolean = true

actual val isFileShareReceiveSupported: Boolean = true

actual val isDeviceSaveSupported: Boolean = true

actual val isDataMigrationSendSupported: Boolean = true

/** 받는 쪽 탐색(`MigrationSourceDiscovery`)이 iOS 에만 있다. */
actual val isDataMigrationReceiveSupported: Boolean = false

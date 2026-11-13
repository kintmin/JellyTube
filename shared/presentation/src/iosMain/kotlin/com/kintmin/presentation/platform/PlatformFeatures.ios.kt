package com.kintmin.presentation.platform

actual val isAudioEditSupported: Boolean = true

/** 이유는 `ios/app/AGENTS.md` §"iOS 에서 제공하지 않는 것". */
actual val isFileShareReceiveSupported: Boolean = false

actual val isDeviceSaveSupported: Boolean = false

/** 보내는 서버가 Android 에만 있다. */
actual val isDataMigrationSendSupported: Boolean = false

actual val isDataMigrationReceiveSupported: Boolean = true

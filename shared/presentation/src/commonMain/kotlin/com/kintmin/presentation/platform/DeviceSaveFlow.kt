package com.kintmin.presentation.platform

enum class DeviceSaveFlow {
    MediaStore,

    /** 등록과 동시에 기본 벨소리도 바뀐다. */
    RingtoneShareSheet,

    RingtoneNeedsNewerOs,
}

expect val deviceSaveFlow: DeviceSaveFlow

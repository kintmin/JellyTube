package com.kintmin.presentation.platform

/** PC(데스크톱)에서 파일을 받는 기능. */
expect val isFileShareReceiveSupported: Boolean

/** 음원을 기기의 벨소리·알람음으로 쓸 수 있게 내보내는 기능. 방식은 [deviceSaveFlow] 다. */
expect val isDeviceSaveSupported: Boolean

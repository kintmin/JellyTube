package com.kintmin.presentation.platform

/**
 * 셋 다 false 다. 이유는 `ios/app/AGENTS.md` §"iOS 에서 제공하지 않는 것" 에 있다.
 * 요약하면 음원 편집은 전면 재작성, PC 수신은 `shared:file-share` 가 JVM 전용 모듈,
 * 기기 저장은 트리밍·변환이 선행 조건이라 음원 편집과 묶인다.
 */
actual val isAudioEditSupported: Boolean = false

actual val isFileShareReceiveSupported: Boolean = false

actual val isDeviceSaveSupported: Boolean = false

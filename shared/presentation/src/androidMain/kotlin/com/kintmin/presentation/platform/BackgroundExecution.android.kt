package com.kintmin.presentation.platform

/** Android 는 앱이 내려가도 프로세스를 곧바로 멈추지 않고, 오래 걸리는 일은 포그라운드 서비스가 붙잡는다. */
actual suspend fun <T> continueInBackgroundWhile(block: suspend () -> T): T = block()

actual val keepsReceivingInBackground: Boolean = true

package com.kintmin.presentation.platform

/** 유예는 길어야 수십 초라 긴 작업의 주된 수단이 아니다. 잠깐 다른 앱을 보고 오는 동안 하던 일을 마치는 정도다. */
actual suspend fun <T> continueInBackgroundWhile(block: suspend () -> T): T =
    withBackgroundExecutionWindow(WINDOW_NAME) { block() }

/** 포그라운드 서비스 같은 장치가 없어 서스펜드되면 서버도 멈춘다. */
actual val keepsReceivingInBackground: Boolean = false

private const val WINDOW_NAME = "background-work"

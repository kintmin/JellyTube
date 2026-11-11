package com.kintmin.presentation.platform

import com.kintmin.presentation.port.withBackgroundExecutionWindow

/**
 * 유예는 길어야 수십 초라 긴 작업의 주된 수단이 아니다. 잠깐 다른 앱을 보고 오는 동안 하던 파일을
 * 마치는 정도다. 그보다 길면 서스펜드되어 연결이 끊기고, 화면에서 다시 시도해 이어 받는다.
 */
actual suspend fun <T> continueInBackgroundWhile(block: suspend () -> T): T =
    withBackgroundExecutionWindow(WINDOW_NAME) { block() }

private const val WINDOW_NAME = "data-migration"

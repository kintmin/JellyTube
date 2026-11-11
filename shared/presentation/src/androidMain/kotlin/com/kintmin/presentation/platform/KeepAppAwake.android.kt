package com.kintmin.presentation.platform

/** 받는 쪽만 쓰는데 Android 는 받는 쪽이 아니다. */
actual suspend fun <T> keepAppAwakeWhile(block: suspend () -> T): T = block()

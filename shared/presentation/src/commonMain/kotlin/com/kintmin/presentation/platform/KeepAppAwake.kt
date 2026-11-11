package com.kintmin.presentation.platform

/** [block] 이 도는 동안 화면이 저절로 꺼지지 않게 하고, 앱이 잠시 내려가도 OS 가 허락하는 만큼 이어서 돈다. */
expect suspend fun <T> keepAppAwakeWhile(block: suspend () -> T): T

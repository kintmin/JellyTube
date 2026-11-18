package com.kintmin.presentation.platform

private const val MIN_MAJOR_VERSION = 26

/** 공유 시트의 "벨소리로 사용" 은 iOS 26 부터 있다. */
internal fun isRingtoneShareSupported(systemVersion: String): Boolean =
    (systemVersion.substringBefore('.').toIntOrNull() ?: 0) >= MIN_MAJOR_VERSION

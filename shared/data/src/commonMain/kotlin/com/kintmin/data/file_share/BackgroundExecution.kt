package com.kintmin.data.file_share

/** 앱이 잠시 내려가도 OS 가 허락하는 만큼 [block] 을 이어서 돌린다. */
internal expect suspend fun <T> continueInBackgroundWhile(block: suspend () -> T): T

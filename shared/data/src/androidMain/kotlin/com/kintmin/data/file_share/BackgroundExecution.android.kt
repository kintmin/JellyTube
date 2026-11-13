package com.kintmin.data.file_share

/** PC 수신은 포그라운드 서비스가 프로세스를 붙잡아 둔다. */
internal actual suspend fun <T> continueInBackgroundWhile(block: suspend () -> T): T = block()

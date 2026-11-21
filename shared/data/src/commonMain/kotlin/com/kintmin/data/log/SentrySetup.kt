package com.kintmin.data.log

import io.sentry.kotlin.multiplatform.Sentry

/** 크래시를 받으려면 DI 보다 먼저 부른다. 이 앞에서 나는 크래시는 받는 곳이 없다. */
fun initSentry(isDebug: Boolean) {
    Sentry.init {
        it.dsn = SENTRY_DSN
        it.environment = if (isDebug) "debug" else "production"
        it.logs.enabled = true
    }
}

private const val SENTRY_DSN =
    "https://31d221d895b2cdd298f3bf1f08b887f0@o4512470047129600.ingest.us.sentry.io/4512470057484288"

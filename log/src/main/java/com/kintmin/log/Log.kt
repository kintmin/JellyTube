package com.kintmin.log

interface Log {
    fun sendLogcatEvent(event: LogcatEvent)
    fun sendFirebaseEvent(event: FirebaseEvent)
}
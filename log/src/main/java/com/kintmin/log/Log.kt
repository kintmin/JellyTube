package com.kintmin.log

interface Log {
    fun sendLogcatEvent(event: LogcatEvent)
    fun setFirebaseConfig(userId: String)
    fun sendFirebaseEvent(event: FirebaseEvent)
}
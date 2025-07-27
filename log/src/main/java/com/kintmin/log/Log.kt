package com.kintmin.log

interface Log {
    fun sendLogcatEvent(event: LogcatEvent)
    fun setFirebaseConfig(rawUserId: String)
    fun sendFirebaseEvent(event: FirebaseEvent)
}
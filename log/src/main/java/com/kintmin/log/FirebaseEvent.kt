package com.kintmin.log

sealed class FirebaseEvent(val logName: String, vararg val params: Pair<String, Any?>) {

}
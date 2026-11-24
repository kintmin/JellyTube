package com.kintmin.presentation.extension

fun Long.toLyricsTimeLabel(): String {
    val safe = coerceAtLeast(0L)
    val hours = safe / 3_600_000L
    val minutes = (safe % 3_600_000L) / 60_000L
    val seconds = (safe % 60_000L) / 1_000L
    val centiseconds = (safe % 1_000L) / 10L
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    val cc = centiseconds.toString().padStart(2, '0')
    return if (hours > 0) "$hours:$mm:$ss.$cc" else "$mm:$ss.$cc"
}

package com.kintmin.presentation.extension

import kotlin.time.Duration

fun Duration.to_hh_colon_mm_colon_ss() = toComponents { hours, minutes, seconds, _ ->
    if (hours != 0L) {
        "$hours:${minutes.toString().padStart(2, '0')}:${
            seconds.toString().padStart(2, '0')
        }"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

/** 밀리초까지 보여준다. 편집 화면처럼 정확한 위치가 필요한 곳에서 쓴다. */
fun Duration.to_hh_colon_mm_colon_ss_dot_sss() = toComponents { hours, minutes, seconds, nanoseconds ->
    val milliseconds = (nanoseconds / 1_000_000).toString().padStart(3, '0')
    if (hours != 0L) {
        "$hours:${minutes.toString().padStart(2, '0')}:${
            seconds.toString().padStart(2, '0')
        }.$milliseconds"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}.$milliseconds"
    }
}

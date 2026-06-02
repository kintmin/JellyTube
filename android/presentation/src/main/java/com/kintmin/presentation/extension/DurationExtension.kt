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
package com.kintmin.presentation.extension

import kotlin.math.roundToLong

/** `String.format` 은 JVM 전용이다. 로케일과 무관하게 소수점은 `.` 이다. */
fun Float.toFixed(digits: Int): String {
    require(digits >= 0) { "digits 는 0 이상이어야 한다: $digits" }

    var factor = 1L
    repeat(digits) { factor *= 10L }

    val scaled = (toDouble() * factor).roundToLong()
    val isNegative = scaled < 0
    val absolute = if (isNegative) -scaled else scaled
    val sign = if (isNegative) "-" else ""

    val integerPart = absolute / factor
    if (digits == 0) return "$sign$integerPart"

    val fractionPart = (absolute % factor).toString().padStart(digits, '0')
    return "$sign$integerPart.$fractionPart"
}

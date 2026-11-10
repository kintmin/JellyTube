package com.kintmin.presentation.extension

import kotlin.math.roundToLong

/**
 * 소수점 [digits] 자리로 반올림한 문자열을 만든다.
 *
 * `String.format` 은 JVM 전용이라 공통 코드에서 쓸 수 없으므로 직접 구현한다.
 * 로케일에 상관없이 소수점은 항상 `.` 이다.
 */
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

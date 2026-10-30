package com.kintmin.presentation.extension

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number

/**
 * "yyyy년 M월 d일 H시 m분 s.SSS초" 형태로 포맷한다.
 */
fun LocalDateTime.toKoreanDateTimeString(): String {
    val millis = (nanosecond / 1_000_000).toString().padStart(3, '0')
    return "${year}년 ${month.number}월 ${day}일 ${hour}시 ${minute}분 ${second}.${millis}초"
}

package com.kintmin.presentation.extension

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number

fun LocalDateTime.toKoreanDateTimeString(): String {
    val millis = (nanosecond / 1_000_000).toString().padStart(3, '0')
    return "${year}년 ${month.number}월 ${day}일 ${hour}시 ${minute}분 ${second}.${millis}초"
}

/** "yyyy년 M월 d일 H시 m분" 형태. 초는 담지 않는다. */
fun LocalDateTime.toKoreanDateTimeStringWithoutSecond(): String =
    "${year}년 ${month.number}월 ${day}일 ${hour}시 ${minute}분"

/** "yyyy.M.d HH:mm" 형태. */
fun LocalDateTime.toDotDateTimeString(): String {
    val hh = hour.toString().padStart(2, '0')
    val mm = minute.toString().padStart(2, '0')
    return "${year}.${month.number}.${day} $hh:$mm"
}

package com.kintmin.domain.extension

/** 서로게이트 쌍을 쪼개면 인코딩할 때 U+FFFD 가 생겨서 그 앞에서 자른다. */
fun String.clampLength(maxLength: Int): String {
    if (length <= maxLength) return this
    val end = if (this[maxLength - 1].isHighSurrogate()) maxLength - 1 else maxLength
    return substring(0, end)
}

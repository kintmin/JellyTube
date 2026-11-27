package com.kintmin.domain.extension

/** 서로게이트 쌍을 쪼개면 인코딩할 때 U+FFFD 가 생겨서 그 앞에서 자른다. */
fun String.clampLength(maxLength: Int): String {
    if (length <= maxLength) return this
    val end = if (this[maxLength - 1].isHighSurrogate()) maxLength - 1 else maxLength
    return substring(0, end)
}

/** 입력 중에 쓰는 자리가 있어 여백을 지우지 않는다 — 지우면 단어 사이 공백을 칠 수 없다. */
fun String.flattenNewlines(): String = replace(NEWLINES, " ")

private val NEWLINES = Regex("[\\r\\n\\u0085\\u2028\\u2029]+")

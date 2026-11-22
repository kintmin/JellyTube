package com.kintmin.desktop.upload

import java.io.File

/** 탐색기처럼 숫자 구간은 수로 비교하고(2 < 10) 대소문자는 가리지 않는다. */
val naturalFileNameOrder: Comparator<File> = Comparator { a, b -> compareNatural(a.name, b.name) }

internal fun compareNatural(a: String, b: String): Int {
    val left = chunks(a)
    val right = chunks(b)
    for (i in 0 until minOf(left.size, right.size)) {
        val x = left[i]
        val y = right[i]
        val result = if (x[0] in '0'..'9' && y[0] in '0'..'9') compareDigits(x, y) else x.compareTo(y, ignoreCase = true)
        if (result != 0) return result
    }
    return (left.size - right.size).takeIf { it != 0 } ?: a.compareTo(b)
}

private fun chunks(name: String): List<String> = CHUNK.findAll(name).map { it.value }.toList()

private fun compareDigits(x: String, y: String): Int {
    val a = x.trimStart('0')
    val b = y.trimStart('0')
    return if (a.length != b.length) a.length - b.length else a.compareTo(b)
}

private val CHUNK = Regex("""\d+|\D+""")

package com.kintmin.data.util

fun <T> assertList(
    actual: List<T>,
    expected: List<T>,
    predicate: (T, T) -> Boolean = { a, e -> a == e }
) {
    var currentIndex = 0
    var errorMessage: String? = null

    if (actual.size > expected.size) {
        errorMessage = """
결과값이 기대값보다 리스트 사이즈가 큽니다.
actual size: ${actual.size} > expected size: ${expected.size}
        """.trimIndent()
    } else if (actual.size < expected.size) {
        errorMessage = """
결과값이 기대값보다 리스트 사이즈가 작습니다.
actual size: ${actual.size} < expected size: ${expected.size}
        """.trimIndent()
    } else {
        while (currentIndex < actual.size && currentIndex < expected.size) {
            if (predicate(actual[currentIndex], expected[currentIndex])) {
                ++currentIndex
            } else {
                errorMessage = """
인덱스 ${currentIndex}에서 값이 일치하지 않습니다.
actual[$currentIndex]=${actual[currentIndex]}
expected[$currentIndex]=${expected[currentIndex]}
            """.trimIndent()
                break
            }
        }
    }

    require(errorMessage == null) {
        """
$errorMessage
[actual]
${actual.joinToString("\n")}
[expected]
${expected.joinToString("\n")}
        """.trimIndent()
    }
}

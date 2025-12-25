package com.kintmin.data.util

fun <T> allCombinations(items: List<T>): List<List<T>> {
    val result = mutableListOf<List<T>>()
    val n = items.size

    for (mask in 1 until (1 shl n)) {
        val combination = mutableListOf<T>()
        for (i in 0 until n) {
            if ((mask and (1 shl i)) != 0) {
                combination.add(items[i])
            }
        }
        result.add(combination)
    }
    return result
}

fun <T> allNullableCombinations(items: List<T>): List<List<T?>> {
    val n = items.size
    val result = mutableListOf<List<T?>>()

    for (mask in 0 until (1 shl n)) {
        val combination = List(n) { index ->
            if ((mask and (1 shl index)) != 0) items[index] else null
        }
        result.add(combination)
    }
    return result
}
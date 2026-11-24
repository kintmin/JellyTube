package com.kintmin.domain.app_update

object AppVersionName {

    fun isNewer(candidate: String, current: String): Boolean {
        val a = numbersOf(candidate)
        val b = numbersOf(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (diff != 0) return diff > 0
        }
        return false
    }

    private fun numbersOf(versionName: String): List<Int> =
        versionName.takeWhile { it.isDigit() || it == '.' }
            .split('.')
            .mapNotNull { it.toIntOrNull() }
}

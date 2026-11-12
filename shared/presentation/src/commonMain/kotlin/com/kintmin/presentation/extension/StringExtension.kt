package com.kintmin.presentation.extension

/** 공백은 양쪽 모두 무시하고, 검색어가 이어진 글자로 들어 있는지 본다. 검색어 글자는 초성만 쳐도 맞는다. */
internal fun String.matchKorean(query: String): Boolean {
    val queryParts = query.filterNot { it.isWhitespace() }.map { Korean(it) }
    if (queryParts.isEmpty()) return true

    val sourceParts = this.filterNot { it.isWhitespace() }.map { Korean(it) }

    return (0..sourceParts.size - queryParts.size).any { start ->
        queryParts.indices.all { offset -> sourceParts[start + offset].matches(queryParts[offset]) }
    }
}

private fun Korean.matches(queryChar: Korean): Boolean =
    initialConsonant == queryChar.initialConsonant &&
        (queryChar.medialVowel == null || queryChar.medialVowel == medialVowel) &&
        (queryChar.finalConsonant == null || queryChar.finalConsonant == finalConsonant)

internal data class Korean(val completedKorean: Char) {
    val initialConsonant: Char
    val medialVowel: Char?
    val finalConsonant: Char?

    init {
        val (initial, medial, final) = splitKorean(completedKorean)
        this.initialConsonant = initial
        this.medialVowel = medial
        this.finalConsonant = final
    }

    companion object {
        private const val INITIAL_LIST = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
        private const val MEDIAL_LIST = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
        private const val FINAL_LIST = " ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"

        fun splitKorean(char: Char): Triple<Char, Char?, Char?> {
            val code = char.code
            return if (code in 0xAC00..0xD7A3) {
                val base = code - 0xAC00
                val initialIndex = base / (21 * 28)
                val medialIndex = (base % (21 * 28)) / 28
                val finalIndex = base % 28

                val initial = INITIAL_LIST[initialIndex]
                val medial = MEDIAL_LIST[medialIndex]
                val finalChar = if (finalIndex == 0) null else FINAL_LIST[finalIndex]

                Triple(initial, medial, finalChar)
            } else {
                Triple(char.lowercaseChar(), null, null)
            }
        }
    }
}
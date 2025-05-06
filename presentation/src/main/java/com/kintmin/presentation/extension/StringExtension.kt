package com.kintmin.presentation.extension

internal fun String.matchKorean(query: String): Boolean {
    if (query.isEmpty()) return true

    val queryParts = query.map { Korean(it) }
    val sourceParts = this.map { Korean(it) }

    var queryIndex = 0

    for (sourceChar in sourceParts) {
        val queryChar = queryParts[queryIndex]

        if (sourceChar.initialConsonant == queryChar.initialConsonant) {
            if (queryChar.medialVowel != null &&
                queryChar.medialVowel != sourceChar.medialVowel
            ) {
                continue
            }

            if (queryChar.finalConsonant != null &&
                queryChar.finalConsonant != sourceChar.finalConsonant
            ) {
                continue
            }

            queryIndex++
            if (queryIndex == queryParts.size) return true
        } else {
            queryIndex = 0
        }
    }

    return false
}

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
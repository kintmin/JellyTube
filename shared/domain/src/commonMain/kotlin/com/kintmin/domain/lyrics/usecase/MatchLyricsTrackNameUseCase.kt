package com.kintmin.domain.lyrics.usecase

/** 가사 검색 결과의 곡명이 제목 안에 들어 있는지 판정한다. */
class MatchLyricsTrackNameUseCase(
    private val buildLyricsSearchQuery: BuildLyricsSearchQueryUseCase,
) {

    // LRCLIB 곡명은 "Through the Night (밤편지)" 처럼 괄호 안에 다른 언어 제목을 담는 경우가 많다.
    private val bracketContentRegex = Regex("""[(\[{（【［](.*?)[)\]}）】］]""")

    operator fun invoke(title: String, trackName: String?): Boolean {
        if (trackName.isNullOrBlank()) return false
        val titleWords = words(title)
        val candidates = listOf(trackName) +
            bracketContentRegex.findAll(trackName).map { it.groupValues[1] }
        return candidates.any { titleWords.containsInOrder(words(it)) }
    }

    private fun words(text: String): List<String> =
        buildLyricsSearchQuery(text).lowercase().split(' ').filter { it.isNotEmpty() }

    // 단순 문자열 포함은 단어 경계를 무시해 "Leo" 가 "Cleopatra" 에 걸리므로 단어 단위로 연속 일치를 본다.
    private fun List<String>.containsInOrder(sub: List<String>): Boolean =
        sub.isNotEmpty() && windowed(sub.size).any { it == sub }
}

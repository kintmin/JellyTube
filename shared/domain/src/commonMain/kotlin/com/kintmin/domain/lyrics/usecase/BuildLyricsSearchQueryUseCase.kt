package com.kintmin.domain.lyrics.usecase

/** 유튜브 제목 원문에서 괄호·구분자·잡음 단어를 걷어 가사 검색어를 만든다. */
class BuildLyricsSearchQueryUseCase {

    private val bracketRegex = Regex("""[(\[{（【].*?[)\]}）】]""")
    // 「」『』 등은 곡명을 감싸는 괄호라 bracketRegex 와 달리 내용을 남긴다.
    private val keepContentSymbolRegex = Regex("""[「」『』《》〈〉<>"“”|｜·]""")
    private val musicVideoRegex = Regex("""music\s+video""", RegexOption.IGNORE_CASE)
    private val whitespaceRegex = Regex("""\s+""")
    private val separatorOnlyRegex = Regex("""[-–—_/:~.,]+""")
    private val apostrophes = charArrayOf('\'', '‘', '’')
    // music·video·audio 단독은 곡명일 수 있어("Video Games") 넣지 않는다.
    private val noiseWords = setOf("mv", "m/v", "official", "lyrics", "가사", "뮤직비디오")

    operator fun invoke(rawTitle: String): String =
        rawTitle
            .replace(bracketRegex, " ")
            .replace(keepContentSymbolRegex, " ")
            .replace(musicVideoRegex, " ")
            .split(whitespaceRegex)
            .map { it.trim(*apostrophes) }
            .filter { it.isNotBlank() }
            .filterNot { it.matches(separatorOnlyRegex) }
            .filterNot { it.lowercase() in noiseWords }
            .joinToString(" ")
}

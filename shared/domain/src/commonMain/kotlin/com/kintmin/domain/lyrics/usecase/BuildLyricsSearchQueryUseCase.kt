package com.kintmin.domain.lyrics.usecase

/** 유튜브 제목 원문에서 괄호·구분자·잡음 단어를 걷어 가사 검색어를 만든다. */
class BuildLyricsSearchQueryUseCase {

    // 괄호 안에도 곡명이나 아티스트 영문 표기가 들어가므로(음율 (UmYull)) 기호만 지우고 내용은 남긴다.
    private val bracketSymbolRegex = Regex("""[(\[{（【［)\]}）】］「」『』《》〈〉<>"“”|｜·]""")
    private val musicVideoRegex = Regex("""\bmusic\s+video\b""", RegexOption.IGNORE_CASE)
    private val whitespaceRegex = Regex("""\s+""")
    private val separatorOnlyRegex = Regex("""[-–—_/:~.,／－：～・、]+""")
    private val apostrophes = charArrayOf('\'', '‘', '’')
    // music·video·audio 단독은 곡명일 수 있어("Video Games") 넣지 않는다.
    private val noiseWords = setOf("mv", "m/v", "official", "lyrics", "가사", "뮤직비디오")

    operator fun invoke(rawTitle: String): String =
        rawTitle
            .replace(bracketSymbolRegex, " ")
            .replace(musicVideoRegex, " ")
            .split(whitespaceRegex)
            .map { it.trim(*apostrophes) }
            .filter { it.isNotBlank() }
            .filterNot { it.matches(separatorOnlyRegex) }
            .filterNot { it.lowercase() in noiseWords }
            .joinToString(" ")
}

package com.kintmin.domain.common_usecase

/** 유튜브 제목 원문에서 괄호·구분자·잡음 단어를 걷어 가사·노래방 검색어를 만든다. */
class BuildSongSearchQueryUseCase {

    // 괄호 안에 피처링·원제 같은 곡 정보가 있어 기호만 지운다. 안의 MV·Official 은 noiseWords 가 거른다.
    private val symbolRegex = Regex("""[()\[\]{}（）【】［］「」『』《》〈〉<>"“”|｜·]""")
    private val musicVideoRegex = Regex("""\bmusic\s+video\b""", RegexOption.IGNORE_CASE)
    private val whitespaceRegex = Regex("""\s+""")
    private val separatorOnlyRegex = Regex("""[-–—_/:~.,／－：～・、]+""")
    private val apostrophes = charArrayOf('\'', '‘', '’')
    // music·video·audio 단독은 곡명일 수 있어("Video Games") 넣지 않는다.
    private val noiseWords = setOf("mv", "m/v", "official", "lyrics", "가사", "뮤직비디오")

    operator fun invoke(rawTitle: String): String =
        rawTitle
            .replace(symbolRegex, " ")
            .replace(musicVideoRegex, " ")
            .split(whitespaceRegex)
            .map { it.trim(*apostrophes) }
            .filter { it.isNotBlank() }
            .filterNot { it.matches(separatorOnlyRegex) }
            .filterNot { it.lowercase() in noiseWords }
            .joinToString(" ")
}

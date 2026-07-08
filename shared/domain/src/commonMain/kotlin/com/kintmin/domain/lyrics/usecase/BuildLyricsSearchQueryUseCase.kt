package com.kintmin.domain.lyrics.usecase

/**
 * 제목 원문으로부터 가사 검색어를 만든다.
 * - 괄호(및 그 안의 내용)를 제거한다. (), [], {}, （） 를 모두 대상으로 한다.
 * - 남은 단어 중 앞 3단어만 사용한다.
 *
 * 예) "sample(ft. temp) to sample" -> "sample to sample"
 */
class BuildLyricsSearchQueryUseCase {

    private val bracketRegex = Regex("""[(\[{（].*?[)\]}）]""")
    private val whitespaceRegex = Regex("""\s+""")

    operator fun invoke(rawTitle: String): String {
        val noBrackets = rawTitle.replace(bracketRegex, " ")
        return noBrackets.split(whitespaceRegex)
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(" ")
    }
}

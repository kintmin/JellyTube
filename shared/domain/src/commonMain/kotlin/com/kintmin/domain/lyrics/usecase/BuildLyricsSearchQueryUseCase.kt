package com.kintmin.domain.lyrics.usecase

/**
 * 제목 원문으로부터 가사 검색어를 만든다.
 * - 잡음 괄호(및 그 안의 내용)를 제거한다. (), [], {}, （）, 【】 를 대상으로 한다.
 * - 곡명이 담기는 괄호/따옴표는 기호만 지우고 내용은 남긴다. 「」, 『』, 《》, 〈〉, <>, ", “”, ‘’
 * - 구분자(-, |, _, /, :, ~ 등)만으로 된 단어와 유튜브 잡음 단어(MV, Official 등)는 버린다.
 *
 * 예) "sample(ft. temp) to sample" -> "sample to sample"
 * 예) "【MV】YOASOBI「アイドル」" -> "YOASOBI アイドル"
 */
class BuildLyricsSearchQueryUseCase {

    private val bracketRegex = Regex("""[(\[{（【].*?[)\]}）】]""")
    private val keepContentSymbolRegex = Regex("""[「」『』《》〈〉<>"“”|｜·]""")
    private val musicVideoRegex = Regex("""music\s+video""", RegexOption.IGNORE_CASE)
    private val whitespaceRegex = Regex("""\s+""")
    private val separatorOnlyRegex = Regex("""[-–—_/:~.,]+""")
    private val apostrophes = charArrayOf('\'', '‘', '’')
    private val noiseWords = setOf("mv", "m/v", "official", "lyrics", "가사", "뮤직비디오")

    operator fun invoke(rawTitle: String): String =
        rawTitle
            .replace(bracketRegex, " ")
            .replace(keepContentSymbolRegex, " ")
            .replace(musicVideoRegex, " ")
            .split(whitespaceRegex)
            // 'Butter' 처럼 단어를 감싼 따옴표만 벗긴다. (don't 같은 단어 안 아포스트로피는 유지)
            .map { it.trim(*apostrophes) }
            .filter { it.isNotBlank() }
            .filterNot { it.matches(separatorOnlyRegex) }
            .filterNot { it.lowercase() in noiseWords }
            .joinToString(" ")
}

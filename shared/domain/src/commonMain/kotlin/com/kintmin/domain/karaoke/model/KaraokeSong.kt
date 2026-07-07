package com.kintmin.domain.karaoke.model

/**
 * 노래방 검색 결과 한 곡.
 * [number]가 노래방 번호(TJ 브랜드면 TJ 번호)다.
 */
data class KaraokeSong(
    val number: String,
    val title: String,
    val singer: String,
)

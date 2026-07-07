package com.kintmin.data.network.model

import kotlinx.serialization.Serializable

/**
 * Manana 노래방 오픈 API 응답 항목.
 * `no`가 노래방 번호(brand=tj면 TJ 번호)다.
 */
@Serializable
data class KaraokeSongDto(
    val brand: String? = null,
    val no: String,
    val title: String? = null,
    val singer: String? = null,
    val composer: String? = null,
    val lyricist: String? = null,
    val release: String? = null,
)

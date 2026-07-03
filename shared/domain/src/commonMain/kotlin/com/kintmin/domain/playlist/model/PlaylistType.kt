package com.kintmin.domain.playlist.model

/**
 * 플레이리스트 종류.
 * - TOTAL(전체), UNCATEGORIZED(미분류), FAVORITE(즐겨찾기): 시스템이 관리하는 플레이리스트.
 * - USER: 사용자가 만든 일반 플레이리스트.
 *
 * 시스템 플레이리스트 판별은 id 매직넘버가 아니라 이 type으로 한다.
 */
enum class PlaylistType(val defaultName: String) {
    TOTAL("전체"),
    UNCATEGORIZED("미분류"),
    FAVORITE("즐겨찾기"),
    USER(""),
    ;

    val isSystem: Boolean get() = this != USER
}

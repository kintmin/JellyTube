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

    /** 다운로드·가져오기한 곡을 넣을 수 있는가. 전체·즐겨찾기는 곡을 직접 넣는 곳이 아니다. */
    val canBeDownloadTarget: Boolean get() = this == USER || this == UNCATEGORIZED
}

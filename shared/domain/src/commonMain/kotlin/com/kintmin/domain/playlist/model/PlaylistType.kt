package com.kintmin.domain.playlist.model

/**
 * 플레이리스트 종류.
 * - TOTAL(전체), UNCATEGORIZED(미분류), FAVORITE(즐겨찾기): 시스템이 관리하는 플레이리스트.
 * - USER: 사용자가 만든 일반 플레이리스트.
 * - SECRET: 비밀모드에서 사용자가 만든 플레이리스트. 비밀 화면 밖에는 보이지 않는다.
 * - SECRET_FAVORITE: 비밀 곡의 즐겨찾기. 일반 즐겨찾기와 섞이지 않게 통째로 나눠 둔다.
 * - SECRET_TOTAL: 비밀 곡의 전체. **비밀 곡의 정의가 이것이다** — 일반 곡이 TOTAL 에 담긴 것과
 *   같은 자리이고, 비밀 곡은 TOTAL 이 아니라 여기 담겨 격리가 저장에서 지켜진다.
 *
 * 시스템 플레이리스트 판별은 id 매직넘버가 아니라 이 type으로 한다.
 */
enum class PlaylistType(val defaultName: String) {
    TOTAL("전체"),
    UNCATEGORIZED("미분류"),
    FAVORITE("즐겨찾기"),
    USER(""),
    SECRET(""),
    SECRET_FAVORITE("찜한 비밀 곡"),
    SECRET_TOTAL("비밀 전체"),
    ;

    /**
     * 비밀모드에서만 드러나는가. 재생 큐 비우기·딥링크 차단처럼 **플레이리스트 id 만 들고 있는
     * 자리**가 이것으로 판정한다 — 그래서 비밀 곡은 한 컨테이너 안에 섞이지 않고 타입으로 갈린다.
     */
    val isSecret: Boolean get() = this == SECRET || this == SECRET_FAVORITE || this == SECRET_TOTAL

    val isSystem: Boolean
        get() = this == TOTAL || this == UNCATEGORIZED || this == FAVORITE ||
            this == SECRET_FAVORITE || this == SECRET_TOTAL

    /**
     * 시스템이 채우고 비우는가. 즐겨찾기는 시스템 플레이리스트지만 사용자가 직접 넣고 빼므로
     * 여기 들어가지 않는다 — `isSystem` 으로 트랙 삭제를 막으면 즐겨찾기에서 곡을 뺄 수 없다.
     */
    val isAutoManaged: Boolean get() = this == TOTAL || this == UNCATEGORIZED || this == SECRET_TOTAL

    /** 다운로드·가져오기한 곡을 넣을 수 있는가. 전체·즐겨찾기는 곡을 직접 넣는 곳이 아니다. */
    val canBeDownloadTarget: Boolean get() = this == USER || this == UNCATEGORIZED || this == SECRET

    /** 담긴 곳을 가리지 않고 그 영역의 곡을 전부 담는가. 골라 보내는 단위가 아니다. */
    val containsEveryTrack: Boolean get() = this == TOTAL || this == SECRET_TOTAL

    fun isVisible(isSecretModeEnabled: Boolean): Boolean = !isSecret || isSecretModeEnabled
}

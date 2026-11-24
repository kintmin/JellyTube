package com.kintmin.domain.data_migration.model

/**
 * 받는 기기가 정한 받는 방식.
 *
 * **[mergingPlaylistSourceIds] 가 null 이면 이름과 종류가 같은 플레이리스트를 모두 합친다** — 내
 * 기기에 내 백업을 되돌리는 경우다. 다른 기기에서 받을 때는 받는 사람이 고른 것만 합치고 나머지는
 * 새로 만든다.
 */
data class MigrationChoice(
    val mergingPlaylistSourceIds: Set<Int>? = null,
    /** 곡만 온 전송에서 곡을 담을 받는 기기 플레이리스트. null 이면 미분류로 간다. */
    val audioMediaDestinationPlaylistId: Int? = null,
    /**
     * 비밀 플레이리스트와 거기에만 담긴 곡을 받을지. 받지 않으면 나머지만 들어온다.
     *
     * **기본이 받는 쪽인 것은 내 백업 되돌리기 때문이다.** 다른 기기에서 받을 때는 받는 사람이
     * 비밀모드를 켜 가며 고르고, 켜지 않으면 비밀은 들어오지 않는다.
     */
    val includesSecret: Boolean = true,
)

package com.kintmin.domain.data_migration.model

/** 보내는 기기가 내보낼 범위. */
sealed interface MigrationScope {

    /**
     * 기기를 옮기는 동기화. 설정과 걸음 기록까지 그대로 간다.
     *
     * [includesSecret] 가 false 면 비밀 플레이리스트와 거기에만 담긴 곡이 빠진다.
     */
    data class Everything(val includesSecret: Boolean) : MigrationScope

    /** 고른 플레이리스트와 그 안의 곡만. */
    data class Playlists(val playlistIds: Set<Int>) : MigrationScope

    /** 고른 곡만. 어느 플레이리스트에 담을지는 받는 기기가 정한다. */
    data class AudioMedias(val audioMediaIds: Set<Int>) : MigrationScope
}

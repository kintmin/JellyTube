package com.kintmin.domain.data_migration.model

import com.kintmin.domain.playlist.model.PlaylistType

/** 곡 수·재생시간·대표 이미지는 소속에서 다시 계산하므로 싣지 않는다. 커스텀 이미지만 싣는다. */
data class MigrationPlaylist(
    val id: Int,
    val name: String,
    val description: String,
    val customImageFileName: String?,
    val sequence: Int,
    val type: PlaylistType,
    val createdTimeMillis: Long,
)

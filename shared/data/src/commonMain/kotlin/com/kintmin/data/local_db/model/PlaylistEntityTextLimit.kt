package com.kintmin.data.local_db.model

import com.kintmin.domain.playlist.PlaylistTextLimit

/** 유튜브·다른 기기 어느 출처도 길이를 보장하지 않아 넣기 전에 자른다. */
internal fun PlaylistEntity.clampText(): PlaylistEntity = copy(
    name = PlaylistTextLimit.clampTitle(name),
    description = PlaylistTextLimit.clampDescription(description),
)

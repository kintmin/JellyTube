package com.kintmin.data.local_db.model

import com.kintmin.domain.audio_media.AudioMediaTextLimit

/** 유튜브·파일 태그·다른 기기 어느 출처도 길이를 보장하지 않아 넣기 전에 자른다. */
internal fun AudioMediaEntity.clampText(): AudioMediaEntity = copy(
    name = AudioMediaTextLimit.clampName(name),
    artist = AudioMediaTextLimit.clampArtist(artist),
    description = AudioMediaTextLimit.clampDescription(description),
)

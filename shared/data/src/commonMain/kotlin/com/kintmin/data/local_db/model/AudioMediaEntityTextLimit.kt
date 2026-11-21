package com.kintmin.data.local_db.model

import com.kintmin.domain.audio_media.AudioMediaTextLimit

/** 새 행을 넣는 모든 경로가 거친다. 출처(유튜브·파일 태그·다른 기기)가 길이를 보장하지 않는다. */
internal fun AudioMediaEntity.clampText(): AudioMediaEntity = copy(
    name = AudioMediaTextLimit.clampName(name),
    artist = AudioMediaTextLimit.clampArtist(artist),
    description = AudioMediaTextLimit.clampDescription(description),
)

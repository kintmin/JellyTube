package com.kintmin.domain.audio_track.model

/**
 * "전체"·"즐겨찾기" 의 곡 목록에서 비밀 플레이리스트의 곡을 어떻게 다룰지. 그 둘이 아닌
 * 플레이리스트의 목록은 이 값과 무관하게 그대로다.
 */
enum class SecretTrackFilter {
    /** 뺀다. 화면이 읽을 때의 기본값이다. */
    EXCLUDE,

    /** 함께 돌려준다. 비밀 플레이리스트에 담을 후보를 고를 때다. */
    INCLUDE,

    /** 비밀 곡만 돌려준다. 비밀 화면의 '찜' 이 이것으로 즐겨찾기를 읽는다. */
    ONLY,
}

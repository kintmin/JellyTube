package com.kintmin.domain.audio_track.model

/**
 * "전체"·"즐겨찾기" 의 곡 목록에서 비밀 플레이리스트의 곡을 어떻게 다룰지. 그 둘이 아닌
 * 플레이리스트의 목록은 이 값과 무관하게 그대로다.
 */
enum class SecretTrackFilter {
    EXCLUDE,
    INCLUDE,
    ONLY,
}

package com.kintmin.domain.youtube

enum class YoutubeUrlKind {
    VIDEO,
    PLAYLIST,

    /** 재생목록에서 연 영상. 그 곡만 받을지 재생목록 전체를 받을지 고른다. */
    VIDEO_IN_PLAYLIST,
    NOT_DOWNLOADABLE,
}

package com.kintmin.domain.youtube.model

data class YoutubePlaylistEntry(
    val videoId: String,
    val title: String,
    val durationSeconds: Long?,
    /** 비공개·삭제된 영상도 목록에는 남는다. 받을 수 없으니 미리 가른다. */
    val isAvailable: Boolean,
) {
    val url: String get() = "https://www.youtube.com/watch?v=$videoId"
}

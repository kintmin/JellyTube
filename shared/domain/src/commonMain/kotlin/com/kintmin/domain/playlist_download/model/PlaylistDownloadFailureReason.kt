package com.kintmin.domain.playlist_download.model

enum class PlaylistDownloadFailureReason {
    UNAVAILABLE,
    DOWNLOAD_FAILED,

    /** 로그인해야 볼 수 있어 다시 받아도 받을 수 없다. */
    AGE_RESTRICTED,
}

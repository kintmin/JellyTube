package com.kintmin.domain.playlist_download.model

enum class PlaylistDownloadStage {
    DOWNLOAD,

    /** 다 받은 뒤 유튜브 순서대로 한 번에 담는다. 동시에 받아 끝나는 순서가 섞이기 때문이다. */
    ADD_TO_PLAYLIST,
}

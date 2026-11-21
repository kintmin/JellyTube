package com.kintmin.presentation.ui.playlist_download

sealed interface PlaylistDownloadIntent {
    data object OnClickStart : PlaylistDownloadIntent

    /** Wi-Fi 가 아닌데도 받겠다고 했다. */
    data object OnConfirmCellular : PlaylistDownloadIntent
    data object OnDismissCellular : PlaylistDownloadIntent
    data object OnClickStop : PlaylistDownloadIntent

    /** 목록을 다시 읽어 남은 곡을 보인다. 받은 곡은 건너뛰므로 다시 받으면 빠진 곡만 받는다. */
    data object OnClickRetry : PlaylistDownloadIntent
    data object OnClickOpenPlaylist : PlaylistDownloadIntent

    /** 나가기 확인 다이얼로그에서 나가기를 골랐다. */
    data object OnConfirmExit : PlaylistDownloadIntent
}

package com.kintmin.presentation.ui.playlist_download

sealed interface PlaylistDownloadIntent {
    data object OnClickStart : PlaylistDownloadIntent

    /** Wi-Fi 가 아닌데도 받겠다고 했다. */
    data object OnConfirmCellular : PlaylistDownloadIntent
    data object OnDismissCellular : PlaylistDownloadIntent
    data object OnClickStop : PlaylistDownloadIntent

    /** 멈췄거나 일부를 받지 못했을 때 빠진 곡만 다시 받는다. 막혔거나 읽지 못했을 때는 처음부터 다시 읽는다. */
    data object OnClickRetry : PlaylistDownloadIntent
    data object OnClickOpenPlaylist : PlaylistDownloadIntent

    /** 나가기 확인 다이얼로그에서 나가기를 골랐다. */
    data object OnConfirmExit : PlaylistDownloadIntent
}

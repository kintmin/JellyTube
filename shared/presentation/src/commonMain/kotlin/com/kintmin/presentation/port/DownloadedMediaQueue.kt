package com.kintmin.presentation.port

import com.kintmin.domain.audio_media.model.DownloadedAudioMediaResult
import com.kintmin.presentation.port.model.toMediaControlData

/** 받은 곡을 전체와 다운로드 대상 플레이리스트의 큐에 넣는다. 지금 그 플레이리스트를 듣고 있지 않으면 포트가 넘긴다. */
fun MediaControllerManager.addDownloadedMedia(result: DownloadedAudioMediaResult) {
    val mediaControlData = result.audioMedia.toMediaControlData()
    listOf(result.totalPlaylistId, result.playlistIdOnDownload).distinct().forEach { playlistId ->
        if (result.shouldInsertAtTopOnDownload) {
            tryAddFirstMediaItem(playlistId, mediaControlData)
        } else {
            tryAddLastMediaItem(playlistId, mediaControlData)
        }
    }
}

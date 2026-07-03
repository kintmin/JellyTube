package com.kintmin.domain.audio_media.model

data class AddedAudioMedia(
    val audioMedia: AudioMedia,
    val totalPlaylistMediaCount: Int,
    val totalPlaylistId: Int,
    val resolvedPlaylistIdOnDownload: Int,
)

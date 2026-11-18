package com.kintmin.domain.audio_media.model

import com.kintmin.domain.audio_media.model.AudioMedia

data class DownloadedAudioMediaResult(
    val audioMedia: AudioMedia,
    val playlistIdOnDownload: Int,
    val totalPlaylistId: Int,
    val shouldInsertAtTopOnDownload: Boolean,
)

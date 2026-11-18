package com.kintmin.domain.audio_media.model

import com.kintmin.domain.audio_media.model.AudioMedia

data class ImportedAudioMediaResult(
    val audioMedia: AudioMedia,
    val playlistIdOnDownload: Int,
)

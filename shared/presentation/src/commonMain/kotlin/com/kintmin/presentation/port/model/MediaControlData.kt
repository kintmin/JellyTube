package com.kintmin.presentation.port.model

import com.kintmin.domain.audio_media.model.AudioMedia

data class MediaControlData(
    val mediaId: String,
    val mediaFileUri: String,
    val mediaTitle: String,
    val mediaDescription: String,
    val mediaArtist: String,
    val mediaDurationMs: Long,
    val mediaArtworkFileUri: String?,
)

fun AudioMedia.toMediaControlData() = MediaControlData(
    mediaId = id.toString(),
    mediaFileUri = playbackFileFullPath,
    mediaTitle = name,
    mediaDescription = description,
    mediaArtist = artist,
    mediaDurationMs = playbackDuration?.inWholeMilliseconds ?: 0L,
    mediaArtworkFileUri = imageFileFullPath,
)

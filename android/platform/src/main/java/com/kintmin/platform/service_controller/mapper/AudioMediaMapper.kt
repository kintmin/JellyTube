package com.kintmin.platform.service_controller.mapper

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.platform.service_controller.model.MediaControlData
import java.io.File

internal fun MediaControlData.toMediaItem() = MediaItem.Builder()
    .setMediaId(mediaId)
    .setUri(mediaFileUri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(mediaTitle)
            .setDescription(mediaDescription)
            .setArtist(mediaArtist)
            .setDurationMs(mediaDurationMs)
            .apply {
                mediaArtworkFileUri?.let {
                    setArtworkUri(Uri.fromFile(File(it)))
                }
            }
            .build()
    )
    .build()

internal fun MediaItem.toMediaControlData() = MediaControlData(
    mediaId = mediaId,
    mediaFileUri = localConfiguration?.uri.toString(),
    mediaTitle = mediaMetadata.title.toString(),
    mediaDescription = mediaMetadata.description.toString(),
    mediaArtist = mediaMetadata.artist.toString(),
    mediaDurationMs = mediaMetadata.durationMs ?: 0L,
    mediaArtworkFileUri = mediaMetadata.artworkUri?.path,
)

internal fun AudioMedia.toMediaControlData() = MediaControlData(
    mediaId = id.toString(),
    mediaFileUri = audioFileFullPath,
    mediaTitle = name,
    mediaDescription = description,
    mediaArtist = artist,
    mediaDurationMs = audioDuration?.inWholeMilliseconds ?: 0L,
    mediaArtworkFileUri = imageFileFullPath,
)

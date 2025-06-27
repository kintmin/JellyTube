package com.kintmin.platform.mapper

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kintmin.domain.audio_media.model.AudioMedia
import java.io.File

internal fun AudioMedia.toMediaItem() = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(audioFileFullPath)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(name)
            .setDescription(description)
            .setArtist(artist)
            .apply {
                imageFileFullPath?.let {
                    setArtworkUri(Uri.fromFile(File(it)))
                }
            }
            .build()
    )
    .build()

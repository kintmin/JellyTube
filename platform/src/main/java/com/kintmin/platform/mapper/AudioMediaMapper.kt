package com.kintmin.platform.mapper

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import java.io.File

internal fun PlaylistTrackAggregate.toMediaItem() = MediaItem.Builder()
    .setMediaId(audioMedia.id.toString())
    .setUri(audioMedia.audioFileFullPath)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(audioMedia.name)
            .setDescription(audioMedia.description)
            .setArtist(audioMedia.artist)
            .apply {
                audioMedia.imageFileFullPath?.let {
                    setArtworkUri(Uri.fromFile(File(it)))
                }
            }
            .build()
    )
    .build()
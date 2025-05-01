package com.kintmin.presentation.ui.playlist_detail.list

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kintmin.domain.model.AudioMedia
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PlaylistDetailListItemUiState(
    val id: Int,
    val mediaName: String,
    val artist: String,
    val audioDuration: Duration?,
    val description: String,
    val audioFileFullPath: String,
    val imageFileFullPath: String?,
    val sequence: Int,
) {
    val subTitle: String get() = "$artist | $audioDurationString | $description"

    private val audioDurationString: String
        get() = audioDuration?.to_hh_colon_mm_colon_ss() ?: "알 수 없음"

    companion object {
        fun getMock(index: Int = 1) = PlaylistDetailListItemUiState(
            id = index,
            mediaName = "미디어",
            artist = "아티스트",
            audioDuration = 500.seconds,
            description = "설명설명설명설명",
            audioFileFullPath = "",
            imageFileFullPath = "",
            sequence = index
        )

        fun getMockList() = List(5) { index -> getMock(index) }
    }
}

internal fun AudioMedia.toUiModel() = PlaylistDetailListItemUiState(
    id = id,
    mediaName = mediaName,
    artist = artist,
    audioDuration = audioDuration,
    description = description,
    audioFileFullPath = audioFileFullPath,
    imageFileFullPath = imageFileFullPath,
    sequence = audioMediaSequence,
)

internal fun PlaylistDetailListItemUiState.toMediaItem() = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(audioFileFullPath)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(mediaName)
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
package com.kintmin.presentation.ui.playlist_edit.list

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PlaylistEditListItemUiState(
    val id: Int,
    val mediaName: String,
    val artist: String,
    val audioDuration: Duration?,
    val description: String,
    val audioFileFullPath: String,
    val imageFileFullPath: String?,
    val sequence: Int,
    val isChecked: Boolean,
    val source: String,
) {
    val subTitle: String get() = "$artist | $audioDurationString | $description"

    private val audioDurationString: String
        get() = audioDuration?.to_hh_colon_mm_colon_ss() ?: "알 수 없음"

    companion object {
        fun getMock(index: Int = 1) = PlaylistEditListItemUiState(
            id = index,
            mediaName = "미디어",
            artist = "아티스트",
            audioDuration = 500.seconds,
            description = "설명설명설명설명",
            audioFileFullPath = "",
            imageFileFullPath = null,
            sequence = index,
            isChecked = index % 2 == 0,
            source = "",
        )

        fun getMockList() = List(5) { index -> getMock(index) }
    }
}

internal fun PlaylistTrackAggregate.toPlaylistEditListItemUiState(isChecked: Boolean) = PlaylistEditListItemUiState(
    id = audioMedia.id,
    mediaName = audioMedia.name,
    artist = audioMedia.artist,
    audioDuration = audioMedia.audioDuration,
    description = audioMedia.description,
    audioFileFullPath = audioMedia.audioFileFullPath,
    imageFileFullPath = audioMedia.imageFileFullPath,
    sequence = audioTrack.trackSequence,
    isChecked = isChecked,
    source = audioMedia.source,
)
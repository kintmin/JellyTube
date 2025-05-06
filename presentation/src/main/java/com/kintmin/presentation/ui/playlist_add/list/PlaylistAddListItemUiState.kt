package com.kintmin.presentation.ui.playlist_add.list

import com.kintmin.domain.model.AudioMedia
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PlaylistAddListItemUiState(
    val id: Int,
    val mediaName: String,
    val artist: String,
    val audioDuration: Duration?,
    val description: String,
    val imageFileFullPath: String?,
    val isChecked: Boolean,
) {
    val subTitle: String get() = "$artist | $audioDurationString | $description"

    private val audioDurationString: String
        get() = audioDuration?.to_hh_colon_mm_colon_ss() ?: "알 수 없음"

    companion object {
        fun getMock(index: Int = 1) = PlaylistAddListItemUiState(
            id = index,
            mediaName = "미디어",
            artist = "아티스트",
            audioDuration = 500.seconds,
            description = "설명설명설명설명",
            imageFileFullPath = "",
            isChecked = index % 2 == 0,
        )

        fun getMockList() = List(5) { index -> getMock(index) }
    }
}

internal fun AudioMedia.toPlaylistAddListItemUiState(isChecked: Boolean) = PlaylistAddListItemUiState(
    id = id,
    mediaName = mediaName,
    artist = artist,
    audioDuration = audioDuration,
    description = description,
    imageFileFullPath = imageFileFullPath,
    isChecked = isChecked,
)
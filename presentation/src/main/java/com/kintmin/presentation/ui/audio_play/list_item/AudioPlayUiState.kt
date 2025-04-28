package com.kintmin.presentation.ui.audio_play.list_item

import com.kintmin.domain.model.AudioMedia
import com.kintmin.platform.model.AudioPlayData
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AudioPlayUiState(
    val id: Int,
    val mediaName: String,
    val artist: String,
    val audioDuration: Duration?,
    val description: String,
    val audioFileFullPath: String,
    val imageFileFullPath: String?,
    val sequence: Int,
) {
    val extraString: String get() = "$artist | $audioDurationString | $description"

    private val audioDurationString: String
        get() = audioDuration?.to_hh_colon_mm_colon_ss() ?: "알 수 없음"

    companion object {
        fun getMock(index: Int = 1) = AudioPlayUiState(
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

internal fun AudioMedia.toUiModel() = AudioPlayUiState(
    id = id,
    mediaName = mediaName,
    artist = artist,
    audioDuration = audioDuration,
    description = description,
    audioFileFullPath = audioFileFullPath,
    imageFileFullPath = imageFileFullPath,
    sequence = audioMediaSequence,
)

internal fun AudioPlayUiState.toTryParcelize() = runCatching {
    AudioPlayData(
        mediaName = mediaName,
        description = description,
        artist = artist,
        audioFileFullPath = audioFileFullPath,
        imageFileFullPath = imageFileFullPath,
    )
}
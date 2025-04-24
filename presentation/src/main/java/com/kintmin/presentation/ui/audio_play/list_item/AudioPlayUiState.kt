package com.kintmin.presentation.ui.audio_play.list_item

import com.kintmin.domain.model.AudioMedia
import com.kintmin.platform.model.AudioPlayData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AudioPlayUiState(
    val id: String,
    val mediaName: String,
    val artist: String,
    val audioDuration: Duration?,
    val description: String,
    val audioFileFullPath: String?,
    val imageFileFullPath: String?,
) {
    val extraString: String get() = "$artist | $audioDurationString | $description"

    private val audioDurationString: String
        get() = audioDuration?.toComponents { hours, minutes, seconds, _ ->
            if (hours != 0L) {
                "$hours:${minutes.toString().padStart(2, '0')}:${
                    seconds.toString().padStart(2, '0')
                }"
            } else {
                "$minutes:${seconds.toString().padStart(2, '0')}"
            }
        } ?: "알 수 없음"

    companion object {
        fun getMock(index: Int = 1) = AudioPlayUiState(
            id = "$index",
            mediaName = "미디어",
            artist = "아티스트",
            audioDuration = 500.seconds,
            description = "설명설명설명설명",
            audioFileFullPath = "",
            imageFileFullPath = "",
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
)

internal fun AudioMedia.toTryParcelize() = runCatching {
    AudioPlayData(
        mediaName = mediaName,
        description = description,
        artist = artist,
        audioFileFullPath = audioFileFullPath!!,
        imageFileFullPath = imageFileFullPath,
    )
}

internal fun AudioPlayUiState.toTryParcelize() = runCatching {
    AudioPlayData(
        mediaName = mediaName,
        description = description,
        artist = artist,
        audioFileFullPath = audioFileFullPath!!,
        imageFileFullPath = imageFileFullPath,
    )
}
package com.kintmin.presentation.ui.audio_play.model

import com.kintmin.domain.model.AudioMediaData
import com.kintmin.platformruntime.model.AudioPlayData
import kotlin.time.Duration

data class AudioPlayUiState(
    val id: String,
    val mediaName: String,
    val artist: String,
    val audioDuration: Duration?,
    val description: String,
    val audioFileFullPath: String,
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
}

internal fun AudioMediaData.toUiModel() = AudioPlayUiState(
    id = id,
    mediaName = mediaName,
    artist = artist,
    audioDuration = audioDuration,
    description = description,
    audioFileFullPath = audioFileFullPath,
    imageFileFullPath = imageFileFullPath,
)

fun AudioPlayUiState.toParcelize() = AudioPlayData(
    mediaName = mediaName,
    description = description,
    artist = artist,
    audioFileFullPath = audioFileFullPath,
    imageFileFullPath = imageFileFullPath,
)
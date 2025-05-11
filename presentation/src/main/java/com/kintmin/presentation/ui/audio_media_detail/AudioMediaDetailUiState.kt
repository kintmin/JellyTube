package com.kintmin.presentation.ui.audio_media_detail

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

data class AudioMediaDetailUiState(
    val audioMediaId: Int,
    val imageFileFullPath: String?,
    val audioMediaName: String,
    val artist: String,
    val playTime: String,
    val audioMediaCreationTime: String,
    val source: String,
    val playlistId: Int,
    val playlistName: String,
    val playlistCreationTime: String,
    val playlistAddedTime: String,
    val audioMediaDescription: String,
) {

    companion object {

        fun getMock(index: Int = 0) = AudioMediaDetailUiState(
            audioMediaId = index,
            imageFileFullPath = null,
            audioMediaName = "새로운 미디어 새로운 미디어 새로운 미디어 새로운 미디어 새로운 미디어",
            artist = "아티스트",
            playTime = "999:99",
            audioMediaCreationTime = "yyyy년 M월 dd일 HH:mm",
            source = "https://preview.com",
            playlistId = index,
            playlistName = "새로운 플레이리스트",
            playlistCreationTime = "yyyy년 M월 dd일 HH:mm",
            playlistAddedTime = "yyyy년 M월 dd일 HH:mm",
            audioMediaDescription = "이것은 최대 100자까지 가능한 미디어 설명입니다. 이것은 최대 100자까지 가능한 미디어 설명입니다. 이것은 최대 100자까지 가능한 미디어 설명입니다.",
        )
    }
}

internal fun PlaylistTrackAggregate.toAudioMediaDetailUiState() = AudioMediaDetailUiState(
    audioMediaId = audioMedia.id,
    imageFileFullPath = audioMedia.imageFileFullPath,
    audioMediaName = audioMedia.name,
    artist = audioMedia.artist,
    playTime = (audioMedia.audioDuration ?: 0.seconds).to_hh_colon_mm_colon_ss(),
    audioMediaCreationTime = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm:ss.SSS").format(audioMedia.createdTime),
    source = audioMedia.source,
    playlistId = playlist.id,
    playlistName = playlist.name,
    playlistCreationTime = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm:ss.SSS").format(playlist.createdTime),
    playlistAddedTime = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm:ss.SSS").format(audioTrack.trackAddedTime),
    audioMediaDescription = audioMedia.description,
)
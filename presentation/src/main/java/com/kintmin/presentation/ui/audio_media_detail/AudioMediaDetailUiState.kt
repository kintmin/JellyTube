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
    val audioMediaDescription: String,
    val playlists: List<Playlist>,
) {
    data class Playlist(
        val playlistId: Int,
        val playlistName: String,
        val playlistCreationTime: String,
        val playlistAddedTime: String,
    )

    companion object {

        fun getMock(index: Int = 1) = AudioMediaDetailUiState(
            audioMediaId = index,
            imageFileFullPath = null,
            audioMediaName = "새로운 미디어 새로운 미디어 새로운 미디어 새로운 미디어 새로운 미디어",
            artist = "아티스트",
            playTime = "999:99",
            audioMediaCreationTime = "yyyy년 M월 dd일 HH:mm",
            source = "https://preview.com",
            playlists = List(5) {
                Playlist(
                    playlistId = index * it,
                    playlistName = "새로운 플레이리스트 $it",
                    playlistCreationTime = "yyyy년 M월 dd일 HH:mm",
                    playlistAddedTime = "yyyy년 M월 dd일 HH:mm",
                )
            },
            audioMediaDescription = "이것은 최대 100자까지 가능한 미디어 설명입니다. 이것은 최대 100자까지 가능한 미디어 설명입니다. 이것은 최대 100자까지 가능한 미디어 설명입니다.",
        )
    }
}

internal fun List<PlaylistTrackAggregate>.toAudioMediaDetailUiState(): AudioMediaDetailUiState {
    val audioMedia = first().audioMedia
    return AudioMediaDetailUiState(
        audioMediaId = audioMedia.id,
        imageFileFullPath = audioMedia.imageFileFullPath,
        audioMediaName = audioMedia.name,
        artist = audioMedia.artist,
        playTime = (audioMedia.audioDuration ?: 0.seconds).to_hh_colon_mm_colon_ss(),
        audioMediaCreationTime = DateTimeFormatter.ofPattern("yyyy년 M월 d일 H시 m분 s.S초")
            .format(audioMedia.createdTime),
        source = audioMedia.source,
        audioMediaDescription = audioMedia.description,
        playlists = this.map { dataList ->
            AudioMediaDetailUiState.Playlist(
                playlistId = dataList.playlist.id,
                playlistName = dataList.playlist.name,
                playlistCreationTime = DateTimeFormatter.ofPattern("yyyy년 M월 d일 H시 m분 s.S초")
                    .format(dataList.playlist.createdTime),
                playlistAddedTime = DateTimeFormatter.ofPattern("yyyy년 M월 d일 H시 m분 s.S초")
                    .format(dataList.audioTrack.trackAddedTime),
            )
        }
    )
}
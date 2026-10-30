package com.kintmin.presentation.ui.audio_media_detail

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.extension.toKoreanDateTimeString
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

data class AudioMediaDetailUiState(
    val audioMediaId: Int,
    val imageFileFullPath: String?,
    val audioMediaName: String,
    val artist: String,
    val playTime: String,
    val audioDurationSeconds: Double?,
    val audioMediaCreationTime: String,
    val source: String,
    val audioMediaDescription: String,
    val hasLyrics: Boolean,
    val tjKaraokeNumber: String?,
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
            audioDurationSeconds = 222.0,
            audioMediaCreationTime = "yyyy년 M월 dd일 HH:mm",
            source = "https://preview.com",
            hasLyrics = false,
            tjKaraokeNumber = "38087",
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
    return firstOrNull()?.audioMedia?.let { audioMedia ->
        AudioMediaDetailUiState(
            audioMediaId = audioMedia.id,
            imageFileFullPath = audioMedia.imageFileFullPath,
            audioMediaName = audioMedia.name,
            artist = audioMedia.artist,
            // 화면에 보여주는 길이는 편집본 기준
            playTime = (audioMedia.playbackDuration ?: 0.seconds).to_hh_colon_mm_colon_ss(),
            // 가사 검색은 음원 길이로 후보를 매칭하므로 편집본이 아니라 원본 길이를 넘긴다
            audioDurationSeconds = audioMedia.audioDuration?.toDouble(DurationUnit.SECONDS),
            audioMediaCreationTime = audioMedia.createdTime.toKoreanDateTimeString(),
            source = audioMedia.source,
            audioMediaDescription = audioMedia.description,
            hasLyrics = audioMedia.lyricFileFullPath != null,
            tjKaraokeNumber = audioMedia.tjKaraokeNumber,
            playlists = this.map { dataList ->
                AudioMediaDetailUiState.Playlist(
                    playlistId = dataList.playlist.id,
                    playlistName = dataList.playlist.name,
                    playlistCreationTime = dataList.playlist.createdTime.toKoreanDateTimeString(),
                    playlistAddedTime = dataList.audioTrack.trackAddedTime.toKoreanDateTimeString(),
                )
            }
        )
    } ?: AudioMediaDetailUiState(
        audioMediaId = -1,
        imageFileFullPath = null,
        audioMediaName = "삭제된 음원",
        artist = "",
        playTime = 0.seconds.to_hh_colon_mm_colon_ss(),
        audioDurationSeconds = null,
        audioMediaCreationTime = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toKoreanDateTimeString(),
        source = "",
        audioMediaDescription = "삭제되었습니다.",
        hasLyrics = false,
        tjKaraokeNumber = null,
        playlists = emptyList(),
    )
}

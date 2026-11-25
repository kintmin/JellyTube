package com.kintmin.presentation.ui.audio_media_detail

import com.kintmin.domain.audio_edit.model.EditedAudioSource
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.presentation.extension.toDotDateTimeString
import com.kintmin.presentation.extension.toKoreanDateTimeStringWithoutSecond
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class AudioMediaDetailUiState(
    val audioMediaId: Int,
    val imageFileFullPath: String?,
    val audioMediaName: String,
    val artist: String,
    val playTime: String,
    val playbackDurationSeconds: Double?,
    val audioDurationSeconds: Double?,
    val audioMediaCreationTime: String,
    val source: String,
    /** 출처를 눌렀을 때 열 웹 주소. */
    val webViewUrl: String?,
    val audioMediaDescription: String,
    val hasLyrics: Boolean,
    val tjKaraokeNumber: String?,
    val hasEditedAudio: Boolean,
    val playlists: List<Playlist>,
    /** 편집본을 백그라운드에서 저장하고 있는지. 저장 중에는 되돌리면 결과가 뒤엉켜 막는다. */
    val isAudioEditSaving: Boolean = false,
    /** 조회 결과가 비어 이 화면이 가리킬 음원이 없는 상태. [audioMediaId] 가 -1 이라 액션을 막아야 한다. */
    val isDeleted: Boolean = false,
    /** 삭제를 실행하는 중. 성공하면 화면을 떠나고, 실패하면 다시 false 가 된다. */
    val isDeleting: Boolean = false,
    /** DB 의 첫 방출 전에는 false. 그 사이의 빈 [playlists] 를 "추가된 플레이리스트 없음" 으로 그리지 않는다. */
    val isLoaded: Boolean = true,
) {
    data class Playlist(
        val playlistId: Int,
        val playlistName: String,
        val playlistAddedTime: String,
    )

    companion object {

        fun getMock(index: Int = 1) = AudioMediaDetailUiState(
            audioMediaId = index,
            imageFileFullPath = null,
            audioMediaName = "새로운 미디어 새로운 미디어 새로운 미디어 새로운 미디어 새로운 미디어",
            artist = "아티스트",
            playTime = "999:99",
            playbackDurationSeconds = 222.0,
            audioDurationSeconds = 222.0,
            audioMediaCreationTime = "2026년 11월 20일 14시 30분",
            source = "https://preview.com",
            webViewUrl = "https://preview.com",
            hasLyrics = false,
            tjKaraokeNumber = "38087",
            hasEditedAudio = true,
            playlists = List(5) {
                Playlist(
                    playlistId = index * it,
                    playlistName = "새로운 플레이리스트 $it",
                    playlistAddedTime = "yyyy.M.d HH:mm",
                )
            },
            audioMediaDescription = "이것은 최대 100자까지 가능한 미디어 설명입니다. 이것은 최대 100자까지 가능한 미디어 설명입니다. 이것은 최대 100자까지 가능한 미디어 설명입니다.",
        )

        /** 삭제된 음원 상태. 변환의 fallback 을 그대로 쓰므로 실제 화면과 어긋나지 않는다. */
        fun getDeletedMock() = emptyList<PlaylistTrackAggregate>().toAudioMediaDetailUiState()
    }
}

internal fun List<PlaylistTrackAggregate>.toAudioMediaDetailUiState(): AudioMediaDetailUiState {
    return firstOrNull()?.audioMedia?.let { audioMedia ->
        AudioMediaDetailUiState(
            audioMediaId = audioMedia.id,
            imageFileFullPath = audioMedia.displayImageFileFullPath,
            audioMediaName = audioMedia.name,
            artist = audioMedia.artist,
            // 화면에 보여주는 길이는 편집본 기준
            playTime = (audioMedia.playbackDuration ?: 0.seconds).to_hh_colon_mm_colon_ss(),
            playbackDurationSeconds = audioMedia.playbackDuration?.toDouble(DurationUnit.SECONDS),
            // 가사 검색은 음원 길이로 후보를 매칭하므로 편집본이 아니라 원본 길이를 넘긴다
            audioDurationSeconds = audioMedia.audioDuration?.toDouble(DurationUnit.SECONDS),
            audioMediaCreationTime = audioMedia.createdTime.toKoreanDateTimeStringWithoutSecond(),
            source = audioMedia.source,
            webViewUrl = EditedAudioSource.webViewUrlOrNull(audioMedia.source),
            audioMediaDescription = audioMedia.description,
            hasLyrics = audioMedia.lyricFileFullPath != null,
            tjKaraokeNumber = audioMedia.tjKaraokeNumber,
            hasEditedAudio = audioMedia.editedAudioFileFullPath != null,
            playlists = this.map { dataList ->
                AudioMediaDetailUiState.Playlist(
                    playlistId = dataList.playlist.id,
                    playlistName = dataList.playlist.name,
                    playlistAddedTime = dataList.audioTrack.trackAddedTime.toDotDateTimeString(),
                )
            }
        )
    } ?: AudioMediaDetailUiState(
        audioMediaId = -1,
        imageFileFullPath = null,
        audioMediaName = "삭제된 음원",
        artist = "",
        playTime = 0.seconds.to_hh_colon_mm_colon_ss(),
        playbackDurationSeconds = null,
        audioDurationSeconds = null,
        audioMediaCreationTime = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toKoreanDateTimeStringWithoutSecond(),
        source = "",
        webViewUrl = null,
        audioMediaDescription = "삭제되었습니다.",
        hasLyrics = false,
        tjKaraokeNumber = null,
        hasEditedAudio = false,
        playlists = emptyList(),
        isDeleted = true,
    )
}

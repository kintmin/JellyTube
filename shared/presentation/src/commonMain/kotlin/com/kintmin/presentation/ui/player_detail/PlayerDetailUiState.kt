package com.kintmin.presentation.ui.player_detail

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_play_setting.model.SleepTimerAnnouncementOptions
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class PlayerDetailUiState(
    val id: String,
    val playlistId: Int?,
    val playlistName: String,
    val title: String,
    val artist: String,
    val currentDuration: Duration,
    val playbackDuration: Duration,
    val imageFileFullPath: String?,
    val isPlaying: Boolean,
    val isShuffling: Boolean,
    val isRepeating: Boolean,
    val playbackSpeed: Float,
    val playbackPitchSemitone: Int,
    val isFavorite: Boolean = false,
    val hasLyrics: Boolean = false,
    val currentLyricLine: String = "",
    val repeatRangeStartDuration: Duration? = null,
    val repeatRangeEndDuration: Duration? = null,
    val isPlaybackSpeedMenuVisible: Boolean = false,
    val isPlaybackPitchMenuVisible: Boolean = false,
    /** 잠들기 타이머가 꺼져 있으면 null. */
    val sleepTimerRemaining: Duration? = null,
    val sleepTimerMinutes: Int = 30,
    val sleepTimerAnnouncement: SleepTimerAnnouncementOptions = SleepTimerAnnouncementOptions(),
    val isSleepTimerMenuVisible: Boolean = false,
    val isSleepTimerAnnouncementMenuVisible: Boolean = false,
) {
    val timeString get() = "${currentDuration.to_hh_colon_mm_colon_ss()} / ${playbackDuration.to_hh_colon_mm_colon_ss()}"

    /** 배지에 띄울 남은 분. 올림해서 1분 미만도 1 로 보인다. 타이머가 꺼져 있으면 null. */
    val sleepTimerRemainingMinutes: Int?
        get() = sleepTimerRemaining?.let { ((it.inWholeSeconds + 59) / 60).toInt() }

    companion object {
        fun getMock() = PlayerDetailUiState(
            id = "temp",
            playlistId = 1,
            playlistName = "재생목록재생목록재생목록재생목록재생목록재생목록재생목록",
            title = "제목제목제목제목제목제목제목제목제목제목제목제목",
            artist = "아티스트아티스트아티스트아티스트아티스트아티스트아티스트아티스트",
            currentDuration = 90.seconds,
            playbackDuration = 300.seconds,
            imageFileFullPath = null,
            isPlaying = true,
            isShuffling = false,
            isRepeating = false,
            playbackSpeed = 1.25f,
            playbackPitchSemitone = 10,
            isFavorite = true,
            hasLyrics = true,
            currentLyricLine = "이 노래의 현재 가사 한 줄이 여기에 표시됩니다",
            repeatRangeStartDuration = 60.seconds,
            repeatRangeEndDuration = 180.seconds,
            isPlaybackSpeedMenuVisible = false,
            isPlaybackPitchMenuVisible = false,
            sleepTimerRemaining = 23.minutes + 12.seconds,
            sleepTimerMinutes = 30,
            sleepTimerAnnouncement = SleepTimerAnnouncementOptions(
                selected = mockAnnouncement(id = 1, name = "잘 자요"),
                selectable = listOf(
                    mockAnnouncement(id = 1, name = "잘 자요"),
                    mockAnnouncement(id = 2, name = "좋은 꿈 꾸세요"),
                ),
            ),
            isSleepTimerMenuVisible = false,
            isSleepTimerAnnouncementMenuVisible = false,
        )

        private fun mockAnnouncement(id: Int, name: String) = AudioMedia(
            id = id,
            source = "source",
            name = name,
            artist = "아티스트",
            description = "",
            audioDuration = 12.seconds,
            audioFileFullPath = "/audio/$id.m4a",
            imageFileFullPath = null,
            createdTime = LocalDateTime(2026, 11, 19, 0, 0),
        )
    }
}

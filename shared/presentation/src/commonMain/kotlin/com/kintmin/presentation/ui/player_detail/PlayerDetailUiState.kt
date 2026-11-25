package com.kintmin.presentation.ui.player_detail

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_play_setting.model.SleepTimerAnnouncementOptions
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.LocalDateTime

/** 세 면이 한 자리를 돌려 쓴다. 전환이 모두 fade 라 Crossfade 의 키가 이 값이어야 한다. */
enum class PlayerArtworkFace { ARTWORK, LYRICS, LYRICS_MISSING }

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
    val lyricLines: List<PlayerLyricLineUiState> = emptyList(),
    val isLyricsSynced: Boolean = false,
    val activeLyricIndex: Int = -1,
    val isLyricsPanelVisible: Boolean = false,
    val isLyricsMissingNoticeVisible: Boolean = false,
    val repeatRangeStartDuration: Duration? = null,
    val repeatRangeEndDuration: Duration? = null,
    val isPlaybackSpeedMenuVisible: Boolean = false,
    val isPlaybackPitchMenuVisible: Boolean = false,
    val sleepTimerRemaining: Duration? = null,
    val sleepTimerMinutes: Int = 30,
    val sleepTimerAnnouncement: SleepTimerAnnouncementOptions = SleepTimerAnnouncementOptions(),
    val isSleepTimerMenuVisible: Boolean = false,
    val isSleepTimerAnnouncementMenuVisible: Boolean = false,
) {
    val timeString get() = "${currentDuration.to_hh_colon_mm_colon_ss()} / ${playbackDuration.to_hh_colon_mm_colon_ss()}"

    /** 첫 타임태그 전과 비싱크 가사에서는 비어 있다. */
    val currentLyricLine: String get() = lyricLines.getOrNull(activeLyricIndex)?.text.orEmpty()

    val currentLyricTranslation: String? get() = lyricLines.getOrNull(activeLyricIndex)?.translation

    /**
     * 아트워크 자리에 세울 면. 뜻을 담는 자리가 둘인 이유는 그 둘의 수명이 다르기 때문이다 —
     * `isLyricsPanelVisible` 은 곡을 넘어가도 남는 선호라서 가사 없는 곡을 지나도 지우지 않고,
     * `isLyricsMissingNoticeVisible` 은 그 곡에 한정된 뜻이라서 곡이 바뀌면 떨어진다.
     */
    val artworkFace: PlayerArtworkFace
        get() = when {
            hasLyrics && isLyricsPanelVisible -> PlayerArtworkFace.LYRICS
            !hasLyrics && isLyricsMissingNoticeVisible -> PlayerArtworkFace.LYRICS_MISSING
            else -> PlayerArtworkFace.ARTWORK
        }

    val showsLyricsPanel: Boolean get() = artworkFace != PlayerArtworkFace.ARTWORK

    /** 올림한다 — 1분 미만도 1 로 보여야 "거의 다 됐다" 가 0 으로 읽히지 않는다. */
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
            lyricLines = listOf(
                PlayerLyricLineUiState(text = "첫 줄은 여기에서 시작합니다"),
                PlayerLyricLineUiState(text = "그 다음 줄이 이어집니다"),
                PlayerLyricLineUiState(
                    text = "今の歌詞の一行がここに表示されます",
                    transliteration = "ima no kashi no ichigyou ga koko ni hyouji saremasu",
                    translation = "이 노래의 현재 가사 한 줄이 여기에 표시됩니다",
                ),
                PlayerLyricLineUiState(text = "다음에 올 줄입니다"),
                PlayerLyricLineUiState(text = "그 뒤로도 가사가 이어집니다"),
            ),
            isLyricsSynced = true,
            activeLyricIndex = 2,
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

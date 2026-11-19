package com.kintmin.presentation.port

import com.kintmin.domain.audio_play_setting.model.RepeatRangeState
import com.kintmin.presentation.port.model.MediaControlData
import kotlin.time.Duration

interface PlaybackController {

    fun initialize()

    val isPlaying: Boolean
    fun pause()
    fun resume()
    fun playPrevious()
    fun playNext()

    val playingMediaItem: MediaControlData?
    val currentPlaylistId: Int?
    val currentPosition: Long?
    val playbackDuration: Long?
    val playbackSpeed: Float
    val playbackPitchSemitone: Int
    val repeatRangeState: RepeatRangeState

    fun seek(duration: Duration)
    fun updateRepeatRange(): Result<Unit>
    fun clearRepeatRange()
    fun repeatRangeIfNeeded()

    fun playFromPlaylist(
        playlistId: Int,
        startMediaId: Int? = null,
        mediaControlDataList: List<MediaControlData>,
    ): Result<Unit>

    fun tryDeleteMediaItem(
        playlistId: Int,
        mediaId: Int,
    ): Result<Unit>

    fun tryAddLastMediaItem(
        playlistId: Int,
        mediaItem: MediaControlData,
    ): Result<Unit>
    fun tryAddFirstMediaItem(
        playlistId: Int,
        mediaItem: MediaControlData,
    ): Result<Unit>

    fun tryReplaceMediaItem(
        mediaId: Int,
        mediaItem: MediaControlData,
    ): Result<Unit>

    fun setShuffleMode(isShuffle: Boolean)
    fun setRepeatMode(isRepeat: Boolean)
    fun setPlaybackSpeed(speed: Float)
    fun setPlaybackPitchSemitone(semitone: Int)

    /**
     * 재생기 자체의 볼륨(0~1). 기기 볼륨이 아니다.
     *
     * 사용자에게 보이는 설정이 아니라 잠들기 타이머가 페이드아웃에 쓰는 수단이다. 깎은 뒤에는
     * 반드시 1 로 되돌려야 한다 — 남겨 두면 다음 재생이 영구히 무음이 된다.
     */
    fun setPlaybackVolume(volume: Float)
}

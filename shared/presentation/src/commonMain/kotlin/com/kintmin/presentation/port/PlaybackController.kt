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

    /** 반복 중이면 마지막 곡에서도 참이다. */
    val hasNext: Boolean

    val playingMediaItem: MediaControlData?
    val currentPlaylistId: Int?
    val currentPosition: Long?
    val playbackDuration: Long?
    val playbackSpeed: Float
    val playbackPitchSemitone: Int
    val repeatRangeState: RepeatRangeState

    fun seek(duration: Duration)
    fun updateRepeatRange()
    fun clearRepeatRange()
    fun repeatRangeIfNeeded()

    fun playFromPlaylist(
        playlistId: Int,
        startMediaId: Int? = null,
        mediaControlDataList: List<MediaControlData>,
    ): Result<Unit>

    /** 재생을 멈추고 큐를 통째로 비운다. 알림·잠금 화면의 곡 정보도 함께 사라진다. */
    fun clearQueue()

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

    /** 재생기 자체의 볼륨(0~1). 기기 볼륨이 아니다. */
    fun setPlaybackVolume(volume: Float)
}

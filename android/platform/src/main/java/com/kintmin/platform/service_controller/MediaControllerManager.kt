package com.kintmin.platform.service_controller

import com.kintmin.platform.service_controller.model.MediaControlData
import kotlin.time.Duration

data class RepeatRangeState(
    val mediaId: String? = null,
    val startDuration: Duration? = null,
    val endDuration: Duration? = null,
)

interface MediaControllerManager {

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

    /**
     * 현재 재생 큐에 해당 음원이 있으면 교체한다. 음원 파일이나 길이가 바뀐 경우(편집본 적용/해제)에 쓴다.
     *
     * playFromPlaylist는 같은 플레이리스트를 다시 재생할 때 큐를 갱신하지 않으므로,
     * 이 호출 없이는 편집 후에도 바뀌기 전 파일이 계속 재생된다.
     * 큐에 없으면 아무 일도 하지 않는다.
     */
    fun tryReplaceMediaItem(
        mediaId: Int,
        mediaItem: MediaControlData,
    ): Result<Unit>

    fun setShuffleMode(isShuffle: Boolean)
    fun setRepeatMode(isRepeat: Boolean)
    fun setPlaybackSpeed(speed: Float)
    fun setPlaybackPitchSemitone(semitone: Int)
}

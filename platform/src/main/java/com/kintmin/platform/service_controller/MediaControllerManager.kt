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

    fun setShuffleMode(isShuffle: Boolean)
    fun setRepeatMode(isRepeat: Boolean)
    fun setPlaybackSpeed(speed: Float)
    fun setPlaybackPitchSemitone(semitone: Int)
}

package com.kintmin.platform.util

import com.kintmin.platform.util.model.MediaControlData
import kotlin.time.Duration

interface MediaControllerManager {

    fun initialize()
    fun release()

    val isPlaying: Boolean
    fun pause()
    fun resume()

    val playingMediaItem: MediaControlData?
    val currentPosition: Long?
    val playbackDuration: Long?

    fun seek(duration: Duration)

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

    fun setShuffleMode(isShuffle: Boolean)
    fun setRepeatMode(isRepeat: Boolean)
}

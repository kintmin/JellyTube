package com.kintmin.platform.util

import android.content.Context
import androidx.media3.common.MediaItem

interface MediaControllerManager {

    fun initialize(context: Context)
    fun release()

    val isPlaying: Boolean
    fun pause()
    fun resume()

    val playingMediaItem: MediaItem?
    val currentPosition: Long?

    fun playFromPlaylist(
        playlistId: Int,
        startMediaId: Int? = null,
    ): Result<Unit>

    fun tryDeleteMediaItem(
        playlistId: Int,
        mediaId: Int,
    ): Result<Unit>

    fun tryAddLastMediaItem(
        playlistId: Int,
        mediaItem: MediaItem,
    ): Result<Unit>

    fun setShuffleMode(isShuffle: Boolean)
    fun setRepeatMode(isRepeat: Boolean)
}

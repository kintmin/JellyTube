package com.kintmin.platform.util

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.kintmin.platform.service.PlaybackService

object MediaControllerManager {
    private var _mediaController: MediaController? = null
    private val mediaController get() = _mediaController ?: throw Exception("미디어 초기화가 필요합니다.")

    val isRepeat get() = mediaController.repeatMode == REPEAT_MODE_ALL
    val isShuffle get() = mediaController.shuffleModeEnabled

    private var _currentPlaylistId = 0

    fun initialize(context: Context) {
        if (_mediaController == null) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            MediaController.Builder(context, sessionToken).buildAsync().let { controllerFuture ->
                controllerFuture.addListener({
                    _mediaController = controllerFuture.get()
                }, MoreExecutors.directExecutor())
            }
        }
    }

    fun release() {
        mediaController.release()
        _mediaController = null
        _currentPlaylistId = 0
    }

    fun pause() {
        if (mediaController.isPlaying) {
            mediaController.pause()
        }
    }

    fun resume() {
        if (!mediaController.isPlaying) {
            mediaController.play()
        }
    }

    fun playFromPlaylist(
        playlistId: Int,
        mediaItemList: List<MediaItem>,
        startMediaId: Int? = null,
    ): Result<Unit> = runCatching {
        if (_currentPlaylistId != playlistId) {
            resetMediaItemList(playlistId, mediaItemList)
        }

        startMediaId?.let {
            seekMediaItem(it).getOrThrow()
        }

        if (!mediaController.isPlaying) {
            mediaController.prepare()
            mediaController.play()
        }
    }

    fun tryDeleteMediaItem(
        playlistId: Int,
        mediaId: Int,
    ): Result<Unit> = runCatching {
        if (_currentPlaylistId == playlistId) {
            val targetIndex = findMediaItem(mediaId).getOrThrow()
            mediaController.removeMediaItem(targetIndex)
        }
    }

    fun tryAddLastMediaItem(
        playlistId: Int,
        mediaItem: MediaItem,
    ): Result<Unit> = runCatching {
        if (_currentPlaylistId == playlistId) {
            mediaController.addMediaItem(mediaItem)
        }
    }

    fun setShuffleMode(isShuffle: Boolean) {
        if (mediaController.shuffleModeEnabled == isShuffle) return
        mediaController.shuffleModeEnabled = isShuffle
        mediaController.prepare()
    }

    fun setRepeatMode(isRepeat: Boolean) {
        mediaController.repeatMode = if (isRepeat) REPEAT_MODE_ALL else REPEAT_MODE_OFF
    }

    private fun resetMediaItemList(playlistId: Int, mediaItemList: List<MediaItem>) {
        _currentPlaylistId = playlistId
        if (mediaController.isPlaying) {
            mediaController.pause()
        }
        if (mediaController.mediaItemCount != 0) {
            mediaController.clearMediaItems()
        }
        mediaController.setMediaItems(mediaItemList)
    }

    private fun seekMediaItem(startMediaId: Int) = runCatching {
        val targetIndex = findMediaItem(startMediaId).getOrThrow()
        if (mediaController.isPlaying) {
            mediaController.pause()
        }
        mediaController.seekTo(targetIndex, 0)
    }

    private fun findMediaItem(mediaId: Int) = runCatching {
        (0 until mediaController.mediaItemCount)
            .first { i -> mediaController.getMediaItemAt(i).mediaId == mediaId.toString() }
    }
}
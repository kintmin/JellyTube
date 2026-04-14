package com.kintmin.platform.util

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.kintmin.platform.util.mapper.toMediaItem
import com.kintmin.platform.service.PlaybackService
import com.kintmin.platform.util.mapper.toMediaControlData
import com.kintmin.platform.util.model.MediaControlData
import javax.inject.Inject
import kotlin.time.Duration

class MediaControllerManagerImpl @Inject constructor(
    private val appContext: Context,
) : MediaControllerManager {

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private var currentPlaylistId: Int? = null

    private fun getMediaController(): MediaController? {
        if (mediaController == null) initialize()
        return mediaController
    }

    override fun initialize() {
        if (mediaController != null || controllerFuture?.isDone == false && controllerFuture?.isCancelled == false) return

        val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync().apply {
            addListener({
                runCatching {
                    mediaController = get()
                }.onFailure {
                    controllerFuture = null
                }
            }, ContextCompat.getMainExecutor(appContext))
        }
    }

    override fun release() {
        try {
            controllerFuture?.let {
                MediaController.releaseFuture(it)
            }
        } finally {
            controllerFuture = null
            mediaController = null
        }
    }

    override val isPlaying: Boolean
        get() = getMediaController()?.isPlaying ?: false

    override fun pause() {
        val mediaController = getMediaController() ?: return
        if (mediaController.isPlaying) {
            mediaController.pause()
        }
    }

    override fun resume() {
        val mediaController = getMediaController() ?: return
        if (!mediaController.isPlaying) {
            mediaController.play()
        }
    }

    override fun playPrevious() {
        val mediaController = getMediaController() ?: return
        mediaController.seekToPreviousMediaItem()
        if (!mediaController.isPlaying) {
            mediaController.play()
        }
    }

    override fun playNext() {
        val mediaController = getMediaController() ?: return
        mediaController.seekToNextMediaItem()
        if (!mediaController.isPlaying) {
            mediaController.play()
        }
    }

    override val playingMediaItem: MediaControlData?
        get() = getMediaController()?.currentMediaItem?.toMediaControlData()
    override val currentPosition: Long?
        get() = getMediaController()?.currentPosition
    override val playbackDuration: Long?
        get() = getMediaController()?.duration

    override fun seek(duration: Duration) {
        getMediaController()?.seekTo(duration.inWholeMilliseconds)
    }

    override fun playFromPlaylist(
        playlistId: Int,
        startMediaId: Int?,
        mediaControlDataList: List<MediaControlData>,
    ): Result<Unit> = runCatching {
        if (currentPlaylistId != playlistId) {
            currentPlaylistId = playlistId
            resetMediaItemList(mediaControlDataList).getOrThrow()
        }

        playbackPlaylist(startMediaId).getOrThrow()
    }

    override fun tryDeleteMediaItem(
        playlistId: Int,
        mediaId: Int,
    ): Result<Unit> = runCatching {
        if (currentPlaylistId == playlistId) {
            val mediaController = getMediaController() ?: return@runCatching
            val targetIndex = findMediaItem(mediaId).getOrThrow()
            mediaController.removeMediaItem(targetIndex)
        }
    }

    override fun tryAddLastMediaItem(
        playlistId: Int,
        mediaItem: MediaControlData,
    ): Result<Unit> = runCatching {
        if (currentPlaylistId == playlistId) {
            val mediaController = getMediaController() ?: return@runCatching
            mediaController.addMediaItem(mediaItem.toMediaItem())
        }
    }

    override fun tryAddFirstMediaItem(
        playlistId: Int,
        mediaItem: MediaControlData,
    ): Result<Unit> = runCatching {
        if (currentPlaylistId == playlistId) {
            val mediaController = getMediaController() ?: return@runCatching
            mediaController.addMediaItem(0, mediaItem.toMediaItem())
        }
    }

    override fun setShuffleMode(isShuffle: Boolean) {
        val mediaController = getMediaController() ?: return
        if (mediaController.shuffleModeEnabled == isShuffle) return
        mediaController.shuffleModeEnabled = isShuffle
        mediaController.prepare()
    }

    override fun setRepeatMode(isRepeat: Boolean) {
        getMediaController()?.repeatMode = if (isRepeat) REPEAT_MODE_ALL else REPEAT_MODE_OFF
    }

    private fun playbackPlaylist(startMediaId: Int?) = runCatching {
        val mediaController = getMediaController() ?: return@runCatching

        startMediaId?.let {
            seekMediaItem(it).getOrThrow()
        }

        if (!mediaController.isPlaying) {
            mediaController.prepare()
            mediaController.play()
        }
    }

    private fun resetMediaItemList(audioMediaList: List<MediaControlData>) = runCatching {
        val mediaController = getMediaController() ?: return@runCatching
        if (mediaController.isPlaying) {
            mediaController.pause()
        }
        if (mediaController.mediaItemCount != 0) {
            mediaController.clearMediaItems()
        }
        mediaController.setMediaItems(audioMediaList.map {
            it.toMediaItem()
        })
    }

    private fun seekMediaItem(startMediaId: Int) = runCatching {
        val mediaController = getMediaController()!!
        val targetIndex = findMediaItem(startMediaId).getOrThrow()
        if (mediaController.isPlaying) {
            mediaController.pause()
        }
        mediaController.seekTo(targetIndex, 0)
    }

    private fun findMediaItem(mediaId: Int) = runCatching {
        val mediaController = getMediaController()!!
        (0 until (mediaController.mediaItemCount))
            .first { i -> mediaController.getMediaItemAt(i).mediaId == mediaId.toString() }
    }
}

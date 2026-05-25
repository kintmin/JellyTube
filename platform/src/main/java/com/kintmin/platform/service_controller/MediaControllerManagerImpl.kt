package com.kintmin.platform.service_controller

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.kintmin.platform.service_controller.mapper.toMediaItem
import com.kintmin.platform.service.PlaybackService
import com.kintmin.platform.service_controller.mapper.toMediaControlData
import com.kintmin.platform.service_controller.model.MediaControlData
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class MediaControllerManagerImpl @Inject constructor(
    private val appContext: Context,
) : MediaControllerManager {

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private var currentPlaylistIdState: Int? = null
    private var playbackSpeedState = 1.0f
    private var playbackPitchSemitoneState = 0
    private var repeatRangeMediaId: String? = null
    private var repeatRangeStart: Duration? = null
    private var repeatRangeEnd: Duration? = null

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
                    mediaController = get().also {
                        applyPlaybackParameters(it)
                    }
                }.onFailure {
                    controllerFuture = null
                }
            }, ContextCompat.getMainExecutor(appContext))
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
    override val currentPlaylistId: Int?
        get() = currentPlaylistIdState
    override val currentPosition: Long?
        get() = getMediaController()?.currentPosition
    override val playbackDuration: Long?
        get() = getMediaController()?.duration
    override val playbackSpeed: Float
        get() = playbackSpeedState
    override val playbackPitchSemitone: Int
        get() = playbackPitchSemitoneState
    override val repeatRangeState: RepeatRangeState
        get() {
            clearRepeatRangeIfMediaChanged()
            return RepeatRangeState(
                mediaId = repeatRangeMediaId,
                startDuration = repeatRangeStart,
                endDuration = repeatRangeEnd,
            )
        }

    override fun seek(duration: Duration) {
        getMediaController()?.seekTo(duration.inWholeMilliseconds)
    }

    override fun updateRepeatRange(): Result<Unit> = runCatching {
        val mediaController = getMediaController() ?: return@runCatching
        val mediaId = mediaController.currentMediaItem?.mediaId ?: return@runCatching
        clearRepeatRangeIfMediaChanged(mediaId)

        val currentDuration = mediaController.currentPosition.milliseconds
        when {
            repeatRangeStart == null -> {
                repeatRangeMediaId = mediaId
                repeatRangeStart = currentDuration
            }

            repeatRangeEnd == null -> {
                val start = repeatRangeStart ?: return@runCatching
                check(currentDuration > start)
                repeatRangeEnd = currentDuration
            }

            else -> clearRepeatRange()
        }
    }

    override fun clearRepeatRange() {
        repeatRangeMediaId = null
        repeatRangeStart = null
        repeatRangeEnd = null
    }

    override fun repeatRangeIfNeeded() {
        val mediaController = getMediaController() ?: return
        val mediaId = mediaController.currentMediaItem?.mediaId ?: return
        clearRepeatRangeIfMediaChanged(mediaId)

        val start = repeatRangeStart ?: return
        val end = repeatRangeEnd ?: return
        if (mediaController.currentPosition.milliseconds >= end) {
            mediaController.seekTo(start.inWholeMilliseconds)
        }
    }

    override fun playFromPlaylist(
        playlistId: Int,
        startMediaId: Int?,
        mediaControlDataList: List<MediaControlData>,
    ): Result<Unit> = runCatching {
        if (currentPlaylistIdState != playlistId) {
            currentPlaylistIdState = playlistId
            resetMediaItemList(mediaControlDataList).getOrThrow()
        }

        playbackPlaylist(startMediaId).getOrThrow()
    }

    override fun tryDeleteMediaItem(
        playlistId: Int,
        mediaId: Int,
    ): Result<Unit> = runCatching {
        if (currentPlaylistIdState == playlistId) {
            val mediaController = getMediaController() ?: return@runCatching
            val targetIndex = findMediaItem(mediaId).getOrThrow()
            mediaController.removeMediaItem(targetIndex)
        }
    }

    override fun tryAddLastMediaItem(
        playlistId: Int,
        mediaItem: MediaControlData,
    ): Result<Unit> = runCatching {
        if (currentPlaylistIdState == playlistId) {
            val mediaController = getMediaController() ?: return@runCatching
            mediaController.addMediaItem(mediaItem.toMediaItem())
        }
    }

    override fun tryAddFirstMediaItem(
        playlistId: Int,
        mediaItem: MediaControlData,
    ): Result<Unit> = runCatching {
        if (currentPlaylistIdState == playlistId) {
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

    override fun setPlaybackSpeed(speed: Float) {
        playbackSpeedState = speed
        applyPlaybackParameters(getMediaController())
    }

    override fun setPlaybackPitchSemitone(semitone: Int) {
        playbackPitchSemitoneState = semitone
        applyPlaybackParameters(getMediaController())
    }

    private fun applyPlaybackParameters(mediaController: MediaController?) {
        mediaController?.playbackParameters = PlaybackParameters(
            playbackSpeedState,
            playbackPitchSemitoneState.toPitchFactor(),
        )
    }

    private fun Int.toPitchFactor(): Float {
        return 2.0.pow(this / 12.0).toFloat()
    }

    private fun clearRepeatRangeIfMediaChanged(mediaId: String? = playingMediaItem?.mediaId) {
        if (repeatRangeMediaId != null && repeatRangeMediaId != mediaId) {
            clearRepeatRange()
        }
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


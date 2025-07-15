package com.kintmin.platform.util

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.platform.mapper.toMediaItem
import com.kintmin.platform.service.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration

class MediaControllerManagerImpl @Inject constructor(
    private val fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
) : MediaControllerManager {
    private val mainScope = MainScope()

    private var fetchDataJob: Job? = null

    private var _mediaController: MediaController? = null
    private var currentPlaylistId = MutableStateFlow<Int?>(null)

    override fun initialize(context: Context) {
        if (_mediaController == null) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            MediaController.Builder(context, sessionToken).buildAsync().let { controllerFuture ->
                controllerFuture.addListener({
                    _mediaController = controllerFuture.get()
                }, ContextCompat.getMainExecutor(context))
            }
        }
    }

    override fun release() {
        _mediaController?.release()
        _mediaController = null
        currentPlaylistId.update { null }
        fetchDataJob?.cancel()
        mainScope.cancel()
    }

    override val isPlaying: Boolean
        get() = _mediaController?.isPlaying ?: false

    override fun pause() {
        if (_mediaController?.isPlaying == true) {
            _mediaController?.pause()
        }
    }

    override fun resume() {
        if (_mediaController?.isPlaying == false) {
            _mediaController?.play()
        }
    }

    override val playingMediaItem: MediaItem?
        get() = _mediaController?.currentMediaItem
    override val currentPosition: Long?
        get() = _mediaController?.currentPosition
    override val playbackDuration: Long?
        get() = _mediaController?.duration

    override fun seek(duration: Duration) {
        _mediaController?.seekTo(duration.inWholeMilliseconds)
    }

    override fun playFromPlaylist(
        playlistId: Int,
        startMediaId: Int?,
    ): Result<Unit> = runCatching {
        if (currentPlaylistId.value != playlistId) {
            val isDataFetchingOtherPlaylist = fetchDataJob?.isCompleted == false
            if (isDataFetchingOtherPlaylist) {
                fetchDataJob!!.cancel()
            }

            fetchDataJob = mainScope.launch {
                resetMediaItemList(playlistId)
                playbackPlaylist(startMediaId)
            }
        }

        val isDataFetching = fetchDataJob?.isCompleted == false
        if (isDataFetching) return@runCatching

        playbackPlaylist(startMediaId)
    }

    override fun tryDeleteMediaItem(
        playlistId: Int,
        mediaId: Int,
    ): Result<Unit> = runCatching {
        if (currentPlaylistId.value == playlistId) {
            val targetIndex = findMediaItem(mediaId).getOrThrow()
            _mediaController?.removeMediaItem(targetIndex)
        }
    }

    override fun tryAddLastMediaItem(
        playlistId: Int,
        mediaItem: MediaItem,
    ): Result<Unit> = runCatching {
        if (currentPlaylistId.value == playlistId) {
            _mediaController?.addMediaItem(mediaItem)
        }
    }

    override fun setShuffleMode(isShuffle: Boolean) {
        if (_mediaController?.shuffleModeEnabled == isShuffle) return
        _mediaController?.shuffleModeEnabled = isShuffle
        _mediaController?.prepare()
    }

    override fun setRepeatMode(isRepeat: Boolean) {
        _mediaController?.repeatMode = if (isRepeat) REPEAT_MODE_ALL else REPEAT_MODE_OFF
    }

    private fun playbackPlaylist(startMediaId: Int?) {
        startMediaId?.let {
            seekMediaItem(it).getOrThrow()
        }

        if (_mediaController?.isPlaying == false) {
            _mediaController?.prepare()
            _mediaController?.play()
        }
    }

    private suspend fun resetMediaItemList(playlistId: Int) = runCatching {
        currentPlaylistId.update { playlistId }

        val mediaItemList = withContext(Dispatchers.IO) {
            fetchAudioMediaListFlowUseCase(playlistId).first().map {
                it.audioMedia.toMediaItem()
            }
        }

        if (_mediaController?.isPlaying == true) {
            _mediaController?.pause()
        }
        if (_mediaController?.mediaItemCount != 0) {
            _mediaController?.clearMediaItems()
        }
        _mediaController?.setMediaItems(mediaItemList)
    }

    private fun seekMediaItem(startMediaId: Int) = runCatching {
        val targetIndex = findMediaItem(startMediaId).getOrThrow()
        if (_mediaController?.isPlaying == true) {
            _mediaController?.pause()
        }
        _mediaController?.seekTo(targetIndex, 0)
    }

    private fun findMediaItem(mediaId: Int) = runCatching {
        (0 until (_mediaController?.mediaItemCount ?: 0))
            .first { i -> _mediaController?.getMediaItemAt(i)?.mediaId == mediaId.toString() }
    }
}
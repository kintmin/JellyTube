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

class MediaControllerManagerImpl @Inject constructor(
    private val fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
) : MediaControllerManager {
    private val mainScope = MainScope()

    private var fetchDataJob: Job? = null

    private var _mediaController: MediaController? = null
    private val mediaController get() = _mediaController ?: throw Exception("미디어 초기화가 필요합니다.")

    val isRepeat get() = mediaController.repeatMode == REPEAT_MODE_ALL
    val isShuffle get() = mediaController.shuffleModeEnabled

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
        mediaController.release()
        _mediaController = null
        currentPlaylistId.update { null }
        mainScope.cancel()
    }

    override fun pause() {
        if (mediaController.isPlaying) {
            mediaController.pause()
        }
    }

    override fun resume() {
        if (!mediaController.isPlaying) {
            mediaController.play()
        }
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
            mediaController.removeMediaItem(targetIndex)
        }
    }

    override fun tryAddLastMediaItem(
        playlistId: Int,
        mediaItem: MediaItem,
    ): Result<Unit> = runCatching {
        if (currentPlaylistId.value == playlistId) {
            mediaController.addMediaItem(mediaItem)
        }
    }

    override fun setShuffleMode(isShuffle: Boolean) {
        if (mediaController.shuffleModeEnabled == isShuffle) return
        mediaController.shuffleModeEnabled = isShuffle
        mediaController.prepare()
    }

    override fun setRepeatMode(isRepeat: Boolean) {
        mediaController.repeatMode = if (isRepeat) REPEAT_MODE_ALL else REPEAT_MODE_OFF
    }

    private fun playbackPlaylist(startMediaId: Int?) {
        startMediaId?.let {
            seekMediaItem(it).getOrThrow()
        }

        if (!mediaController.isPlaying) {
            mediaController.prepare()
            mediaController.play()
        }
    }

    private suspend fun resetMediaItemList(playlistId: Int) = runCatching {
        currentPlaylistId.update { playlistId }

        val mediaItemList = withContext(Dispatchers.IO) {
            fetchAudioMediaListFlowUseCase(playlistId).first().map {
                it.audioMedia.toMediaItem()
            }
        }

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
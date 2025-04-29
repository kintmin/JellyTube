package com.kintmin.platform.util

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.kintmin.platform.service.PlaybackService

object MediaControllerManager {
    private var _mediaController: MediaController? = null

    val isRepeat get() = _mediaController?.repeatMode == REPEAT_MODE_ALL
    val isShuffle get() = _mediaController?.shuffleModeEnabled == true

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

    fun clearMediaItems() {
        _mediaController?.clearMediaItems()
    }

    fun addMedia(mediaItemList: List<MediaItem>) {
        _mediaController?.setMediaItems(mediaItemList)
        _mediaController?.prepare()
    }

    fun isEmpty(): Boolean {
        return (_mediaController?.mediaItemCount ?: 0) > 0
    }

    fun play() {
        _mediaController?.play()
    }

    fun setShuffleMode(isShuffle: Boolean) {
        _mediaController?.shuffleModeEnabled = isShuffle
        _mediaController?.prepare()
    }

    fun setRepeatMode(isRepeat: Boolean) {
        _mediaController?.repeatMode = if (isRepeat) {
            REPEAT_MODE_ALL
        } else {
            REPEAT_MODE_OFF
        }
    }

    fun release() {
        _mediaController?.release()
        _mediaController = null
    }
}
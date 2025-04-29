package com.kintmin.platform.util

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.kintmin.platform.service.PlaybackService

object MediaControllerManager {
    private var _mediaController: MediaController? = null

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
    }

    fun play() {
        _mediaController?.prepare()
        _mediaController?.play()
    }

    fun release() {
        _mediaController?.release()
        _mediaController = null
    }
}
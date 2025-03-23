package com.kintmin.presentation.service

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.kintmin.domain.model.AudioMediaData
import java.io.File

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_GAME) // ducking 방지
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()

        // https://developer.android.com/reference/androidx/media3/session/MediaSession.Callback
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    return super.onCustomCommand(session, controller, customCommand, args)
                }

                // MediaItem 없이 재생 요청 시 준비해야 할 재생목록
                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    return super.onPlaybackResumption(mediaSession, controller)
                }
            })
            .build()
    }

    fun uriToBitmap(uri: Uri): Bitmap? {
        val inputStream = contentResolver.openInputStream(uri)
        return try {
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        } finally {
            inputStream?.close()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mediaData = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            intent?.getParcelableExtra(EXTRA_MEDIA_DATA, AudioMediaData::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_MEDIA_DATA)
        }

        if (mediaData != null) {
            playImmediately(mediaData)
        }

        val mediaDataList = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            intent?.getParcelableArrayListExtra(EXTRA_MEDIA_DATA_LIST, AudioMediaData::class.java)
        } else {
            intent?.getParcelableArrayListExtra(EXTRA_MEDIA_DATA_LIST)
        }

        mediaDataList?.forEach {
            addToPlaylist(it)
        }
        mediaDataList?.firstOrNull()?.let {
            playImmediately(it)
        }

        return super.onStartCommand(intent, flags, startId)
    }


    fun playImmediately(mediaData: AudioMediaData) {
        mediaSession?.apply {
            player.setMediaItem(getMediaItem(mediaData))
            player.prepare()
            player.play()
        }
    }

    fun addToPlaylist(mediaData: AudioMediaData) {
        mediaSession?.apply {
            player.addMediaItem(getMediaItem(mediaData))
            player.prepare()
        }
    }

    fun playSpecificTrack(index: Int) {
        mediaSession?.apply {
            if (index in 0 until player.mediaItemCount) {
                player.seekTo(index, 0L)
                player.play()
            }
        }
    }

    fun playNext() {
        mediaSession?.apply {
            if (player.hasNextMediaItem()) {
                player.seekToNext()
            }
        }
    }

    fun playPrevious() {
        mediaSession?.apply {
            if (player.hasPreviousMediaItem()) {
                player.seekToPrevious()
            }
        }
    }

    private fun getMediaItem(mediaData: AudioMediaData) = MediaItem.Builder()
        .setUri(mediaData.audioFilePath)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(mediaData.title)
                .setDescription(mediaData.description)
                .apply {
                    mediaData.imageFilePath?.let {
                        setArtworkUri(Uri.fromFile(File(it)))
                    }
                }
                .build()
        )
        .build()

    companion object {
        const val EXTRA_MEDIA_DATA = "EXTRA_MEDIA_DATA"
        const val EXTRA_MEDIA_DATA_LIST = "EXTRA_MEDIA_DATA_LIST"
    }
}

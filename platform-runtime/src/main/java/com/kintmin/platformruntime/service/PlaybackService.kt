package com.kintmin.platformruntime.service

import android.content.Intent
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
import com.kintmin.platformruntime.model.AudioPlayData
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

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val audioPlayData = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            intent?.getParcelableExtra(EXTRA_MEDIA_DATA, AudioPlayData::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_MEDIA_DATA)
        }

        if (audioPlayData != null) {
            playImmediately(audioPlayData)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun playImmediately(audioPlayData: AudioPlayData) {
        mediaSession?.apply {
            player.setMediaItem(getMediaItem(audioPlayData))
            player.prepare()
            player.play()
        }
    }

    fun addToPlaylist(audioPlayData: AudioPlayData) {
        mediaSession?.apply {
            player.addMediaItem(getMediaItem(audioPlayData))
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

    private fun getMediaItem(audioPlayData: AudioPlayData) = MediaItem.Builder()
        .setUri(audioPlayData.audioFileFullPath)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(audioPlayData.mediaName)
                .setDescription(audioPlayData.description)
                .apply {
                    audioPlayData.imageFileFullPath?.let {
                        setArtworkUri(Uri.fromFile(File(it)))
                    }
                }
                .build()
        )
        .build()

    companion object {
        const val EXTRA_MEDIA_DATA = "EXTRA_MEDIA_DATA"
    }
}
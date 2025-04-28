package com.kintmin.platform.service

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
import com.kintmin.platform.model.AudioPlayData
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
        if (intent == null) return START_NOT_STICKY

        val audioStartIndex = intent.getIntExtra(EXTRA_AUDIO_START_INDEX, -1)
        if (audioStartIndex == -1) {
            return START_NOT_STICKY
        }

        val shouldClear = intent.getBooleanExtra(EXTRA_SHOULD_CLEAR, false)

        val playlist = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_AUDIO_MEDIA_LIST, AudioPlayData::class.java)
        } else {
            intent.getParcelableArrayListExtra(EXTRA_AUDIO_MEDIA_LIST)
        } ?: arrayListOf()

        setPlaylist(playlist, audioStartIndex, shouldClear)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setPlaylist(
        playlist: ArrayList<AudioPlayData>,
        startIndex: Int = 0,
        shouldClear: Boolean = false
    ) {
        if (shouldClear) {
            resetAudioMediaList(playlist, startIndex)
        } else {
            seekAudioMedia(startIndex)
        }
    }

    private fun seekAudioMedia(startIndex: Int = 0) {
        mediaSession?.apply {
            player.seekTo(startIndex, 0L)
            player.play()
        }
    }

    private fun resetAudioMediaList(
        playlist: ArrayList<AudioPlayData>,
        startIndex: Int = 0,
    ) {
        mediaSession?.apply {
            player.stop()
            player.clearMediaItems()

            val newMediaItems = playlist.map { getMediaItem(it) }
            player.setMediaItems(newMediaItems, startIndex, 0L)
            player.prepare()
            player.play()
        }
    }

    private fun getMediaItem(audioPlayData: AudioPlayData) = MediaItem.Builder()
        .setUri(audioPlayData.audioFileFullPath)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(audioPlayData.mediaName)
                .setDescription(audioPlayData.description)
                .setArtist(audioPlayData.artist)
                .apply {
                    audioPlayData.imageFileFullPath?.let {
                        setArtworkUri(Uri.fromFile(File(it)))
                    }
                }
                .build()
        )
        .build()

    companion object {
        const val EXTRA_AUDIO_MEDIA_LIST = "EXTRA_AUDIO_MEDIA_LIST"
        const val EXTRA_AUDIO_START_INDEX = "EXTRA_AUDIO_START_INDEX"
        const val EXTRA_SHOULD_CLEAR = "EXTRA_SHOULD_CLEAR"
    }
}
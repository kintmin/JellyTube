package com.kintmin.jellytube.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.kintmin.jellytube.intent.mediaSessionPendingIntent
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlaybackService : MediaSessionService(), KoinComponent {

    private val appLog: AppLog by inject()
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
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        appLog.sendDebugLog(
                            DebugLog(
                                "PlaybackService",
                                "${error.errorCodeName} mediaId=${currentMediaItem?.mediaId}: ${error.cause ?: error.message}",
                            ),
                        )
                    }
                })
            }

        // https://developer.android.com/reference/androidx/media3/session/MediaSession.Callback
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(mediaSessionPendingIntent())
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
}

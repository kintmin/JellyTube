package com.kintmin.platform.service

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.legacy.MediaSessionCompat
import androidx.media3.session.legacy.PlaybackStateCompat
import com.kintmin.platform.R
import com.kintmin.platform.notification.NotificationChannelData
import com.kintmin.platform.notification.NotificationData
import com.kintmin.platform.util.MediaControllerManager
import javax.inject.Inject

class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var mediaControllerManager: MediaControllerManager

    private var mediaSession: MediaSession? = null

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    @OptIn(UnstableApi::class) override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_GAME) // ducking 방지
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()

        mediaSession = MediaSessionCompat(this, "PlaybackService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    player.play()
                    updateState(PlaybackStateCompat.STATE_PLAYING)
                }

                override fun onPause() {
                    player.pause()
                    updateState(PlaybackStateCompat.STATE_PAUSED)
                }

                override fun onSeekTo(pos: Long) {
                    player.seekTo(pos)
                    updateState(if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED)
                }

                override fun onSkipToNext() {
                    // 필요시 구현
                }

                override fun onSkipToPrevious() {
                    // 필요시 구현
                }
            })
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                updateState(
                    when (state) {
                        Player.STATE_READY -> if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                        Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
                        else -> PlaybackStateCompat.STATE_BUFFERING
                    }
                )
            }
        })

        // 4) 포그라운드 서비스로 시작
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onDestroy() {
        mediaControllerManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
    private fun updateState(playbackState: Int) {
        val state = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(
                playbackState,
                player.currentPosition,
                /* playbackSpeed= */ 1f,
                SystemClock.elapsedRealtime()
            )
            .build()
        mediaSession.setPlaybackState(state)

        // 알림도 갱신
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_white)
            .setColor(getColor(R.color.notificationAccent))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0,1,2,3)
            )
            .setContentTitle(player.currentMediaItem?.mediaMetadata?.title)
            .setContentText(player.currentMediaItem?.mediaMetadata?.artist)
            .addAction(buildAction(android.R.drawable.ic_media_previous, "Prev") {
                mediaSession.controller.transportControls.skipToPrevious()
            })
            .addAction(buildAction(
                if (player.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (player.isPlaying) "Pause" else "Play"
            ) {
                if (player.isPlaying) mediaSession.controller.transportControls.pause()
                else mediaSession.controller.transportControls.play()
            })
            .addAction(buildAction(android.R.drawable.ic_media_next, "Next") {
                mediaSession.controller.transportControls.skipToNext()
            })
            .addAction(buildAction(android.R.drawable.ic_media_rew, "-15s") {
                mediaSession.controller.transportControls.seekTo(
                    (player.currentPosition - SEEK_INTERVAL_MS).coerceAtLeast(0L)
                )
            })
            .addAction(buildAction(android.R.drawable.ic_media_ff, "+15s") {
                mediaSession.controller.transportControls.seekTo(
                    (player.currentPosition + SEEK_INTERVAL_MS).coerceAtMost(player.duration)
                )
            })
            .build()
            .also { notification ->
                // Update ongoing notification
                NotificationCompat.from(this).notify(NOTIF_ID, notification)
            }
    }

    private fun buildNotification(): Notification {
        // 첫 호출 시용 간단 알림
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_white)
            .setColor(getColor(R.color.notificationAccent))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
            )
            .setContentTitle("Loading…")
            .build()
    }

    private fun buildAction(icon: Int, title: String, intentBlock: () -> Unit): NotificationCompat.Action {
        // MediaButtonReceiver를 통해 PendingIntent 생성
        val intent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this,
            when (title) {
                "Play"  -> PlaybackStateCompat.ACTION_PLAY
                "Pause" -> PlaybackStateCompat.ACTION_PAUSE
                "Next"  -> PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                "Prev"  -> PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                "-15s", "+15s" -> PlaybackStateCompat.ACTION_SEEK_TO
                else    -> PlaybackStateCompat.ACTION_PLAY
            }
        )
        return NotificationCompat.Action.Builder(icon, title, intent).build()
    }
}
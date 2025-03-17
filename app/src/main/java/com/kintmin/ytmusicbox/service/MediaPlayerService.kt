package com.kintmin.ytmusicbox.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.kintmin.ytmusicbox.R

class MediaPlayerService : Service() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()

        createNotificationChannel()

//        val mediaItem = MediaItem.fromUri(Uri.parse("android.resource://$packageName/${R.raw.sample_sound}"))
//        player.setMediaItems(listOf(mediaItem, mediaItem))
//        player.prepare()
//        player.playWhenReady = true
//        mediaSession.setPlayer(player)

        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        player.stop()
        player.release()
        mediaSession.release()
        stopSelf()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    @OptIn(UnstableApi::class)
    private fun createNotification(): Notification {
        //val albumArt = BitmapFactory.decodeResource(resources, R.drawable.sample_art)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            //.setLargeIcon(albumArt)
            .setContentTitle("곡 제목")
            .setContentText("아티스트")
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(mediaSession)
                    .setShowActionsInCompactView(1)
            )
            .build()
    }

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val CHANNEL_NAME = "음악 채널"
        const val NOTIFICATION_ID = 1
    }
}

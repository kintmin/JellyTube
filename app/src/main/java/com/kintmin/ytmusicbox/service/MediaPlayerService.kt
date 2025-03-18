package com.kintmin.ytmusicbox.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.kintmin.ytmusicbox.R
import com.kintmin.ytmusicbox.data.local.LocalFileDataSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MediaPlayerService : Service() {

    @Inject
    lateinit var localFileDataSource: LocalFileDataSource

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val audioId = intent?.getStringExtra(EXTRA_AUDIO_ID)
        if (audioId != null) {
            playAudio(audioId)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        player.stop()
        player.release()
        mediaSession.release()
        stopSelf()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun playAudio(id: String) {
        if (!::localFileDataSource.isInitialized) return
        scope.launch {
            val data = localFileDataSource.getYoutubeData(id).getOrNull() ?: return@launch
            withContext(Dispatchers.Main) {
                val mediaItem = MediaItem.fromUri(data.audioFilePath)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
                mediaSession.setPlayer(player)
                updateNotification(data.title, data.description, data.imageFilePath)
            }
        }
    }

    private fun updateNotification(
        title: String,
        content: String,
        imageFilePath: String?,
    ) {
        val thumbnail = imageFilePath?.let {
            val bitmap = BitmapFactory.decodeFile(it)
            Bitmap.createScaledBitmap(bitmap, 256, 256, false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(title, content, thumbnail))
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    @OptIn(UnstableApi::class)
    private fun createNotification(
        title: String = "곡 제목",
        content: String = "아티스트",
        thumbnail: Bitmap? = null,
    ): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(thumbnail)
            .setContentTitle(title)
            .setContentText(content)
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
        const val EXTRA_AUDIO_ID = "EXTRA_AUDIO_ID"
    }
}

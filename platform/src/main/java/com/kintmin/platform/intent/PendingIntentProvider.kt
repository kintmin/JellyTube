package com.kintmin.platform.intent

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.kintmin.platform.deeplink.DeepLinkConstants

private const val TARGET_ACTIVITY_NAME = "com.kintmin.jellytube.MainActivity"

internal fun Context.mediaSessionPendingIntent() = PendingIntent.getActivity(
    this,
    IntentRequestCode.MEDIA_SESSION_NOTIFICATION,
    Intent().apply {
        action = Intent.ACTION_VIEW
        data = DeepLinkConstants.UriBuilder.playerScreen(DeepLinkConstants.PlayerScreenEntry.Playlist)
        component = ComponentName(
            packageName,
            TARGET_ACTIVITY_NAME,
        )
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

internal fun Context.downloadResultPendingIntent(
    playlistId: Int,
    audioMediaId: Int,
) = PendingIntent.getActivity(
    this,
    IntentRequestCode.DOWNLOAD_RESULT_NOTIFICATION,
    Intent().apply {
        action = Intent.ACTION_VIEW
        data = DeepLinkConstants.UriBuilder.playlistContentScreen(
            playlistId = playlistId,
            audioMediaId = audioMediaId,
        )
        component = ComponentName(
            packageName,
            TARGET_ACTIVITY_NAME,
        )
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

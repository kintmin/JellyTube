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
        data = DeepLinkConstants.UriBuilder.playerBar()
        component = ComponentName(
            packageName,
            TARGET_ACTIVITY_NAME,
        )
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

package com.kintmin.presentation.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

private const val RECIPIENT = "kintmin4@gmail.com"
private const val SUBJECT = "JellyTube App Logs"
private const val CHOOSER_TITLE = "메일 앱 선택"

private fun Context.shareAppLogs(fileFullPaths: List<String>): AppLogShareResult {
    if (fileFullPaths.isEmpty()) return AppLogShareResult.NoLogFiles
    val logFileList = fileFullPaths.map(::File)

    val authority = "$packageName.fileprovider"
    val uriList = ArrayList<Uri>(logFileList.size)
    logFileList.forEach { file ->
        uriList += FileProvider.getUriForFile(this, authority, file)
    }

    val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(RECIPIENT))
        putExtra(Intent.EXTRA_SUBJECT, SUBJECT)
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    return runCatching {
        startActivity(Intent.createChooser(sendIntent, CHOOSER_TITLE))
        AppLogShareResult.Started
    }.getOrElse { throwable ->
        if (throwable is ActivityNotFoundException) AppLogShareResult.NoShareTarget
        else AppLogShareResult.Failed
    }
}

@Composable
actual fun rememberAppLogSharer(): AppLogSharer {
    val context = LocalContext.current
    return remember(context) { AppLogSharer { context.shareAppLogs(it) } }
}

package com.kintmin.ytmusicbox.data.local

import android.content.Context
import android.os.Environment
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kintmin.ytmusicbox.data.local.dto.YoutubeDownloadDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class YoutubeDownloadDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun download(
        youtubeUrl: String,
        videoId: String,
    ) = runCatching {
        withContext(Dispatchers.IO) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            val downloadPath =
                "${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath}/$videoId.mp3"
            withTimeout(10000L) {
                val module = Python.getInstance().getModule("download_youtube_audio")
                val pyResult =
                    module.callAttr("download_audio", youtubeUrl, downloadPath).toString()
                if (pyResult.contains("Exception")) {
                    throw Exception(pyResult)
                } else {
                    YoutubeDownloadDto(
                        title = pyResult,
                        audioFilePath = downloadPath,
                        imageFilePath = "$downloadPath.webp",
                    )
                }
            }
        }
    }
}

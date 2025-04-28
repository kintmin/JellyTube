package com.kintmin.data.python_bridge

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kintmin.data.python_bridge.model.YoutubeDownloadDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

internal class PythonExecutorImpl @Inject constructor(
    @ApplicationContext private val context: Context
): PythonExecutor {

    override suspend fun downloadYoutubeMedia(
        youtubeUrl: String,
        audioDownloadPath: String,
    ) = runCatching {
        withContext(Dispatchers.IO) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            withTimeout(TIME_OUT) {
                val module = Python.getInstance().getModule(FILE_NAME)
                val version = module.callAttr(METHOD_GET_VERSION).toString()
                Log.d("DEBUG", "yt-dlp version: $version")

                val pyResult =
                    module.callAttr(METHOD_DOWNLOAD_AUDIO, youtubeUrl, audioDownloadPath).asList()

                YoutubeDownloadDto(
                    title = pyResult[0].toString(),
                    thumbnailDownloadUrl = pyResult[1].toString(),
                    duration = pyResult[2].toString(),
                    uploader = pyResult[3].toString(),
                    description = pyResult[4].toString(),
                )
            }
        }
    }

    companion object {
        const val TIME_OUT = 60000L
        const val FILE_NAME = "download_youtube_audio"
        const val METHOD_DOWNLOAD_AUDIO = "download_audio"
        const val METHOD_GET_VERSION = "get_version"
    }
}

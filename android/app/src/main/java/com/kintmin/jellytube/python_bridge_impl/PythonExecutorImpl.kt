package com.kintmin.jellytube.python_bridge_impl

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.python_bridge.model.YoutubeDownloadDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class PythonExecutorImpl(
    private val context: Context
) : PythonExecutor {

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
                    title = pyResult.getOrNull(0)?.toString() ?: "알 수 없음",
                    thumbnailDownloadUrl = pyResult.getOrNull(1)?.toString() ?: "",
                    duration = pyResult.getOrNull(2)?.toString() ?: "0",
                    uploader = pyResult.getOrNull(3)?.toString() ?: "알 수 없음",
                    description = pyResult.getOrNull(4)?.toString() ?: "",
                    audioFileNameWithExt = pyResult.getOrNull(5)?.toString().orEmpty(),
                )
            }
        }
    }

    override suspend fun extractYoutubeUrlsFromPlaylist(playlistUrl: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            withTimeout(TIME_OUT) {
                val module = Python.getInstance().getModule(FILE_NAME)
                val pyList = module.callAttr(METHOD_EXTRACT_VIDEO_URLS_FROM_PLAYLIST, playlistUrl).asList()
                pyList.mapNotNull { it?.toString() }
            }
        }
    }

    private companion object {
        const val TIME_OUT = 30000L
        const val FILE_NAME = "download_youtube_audio"
        const val METHOD_GET_VERSION = "get_version"
        const val METHOD_DOWNLOAD_AUDIO = "download_audio"
        const val METHOD_EXTRACT_VIDEO_URLS_FROM_PLAYLIST = "extract_video_urls_from_playlist"
    }
}

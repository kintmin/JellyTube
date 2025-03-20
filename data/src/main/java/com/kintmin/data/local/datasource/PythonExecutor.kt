package com.kintmin.data.local.datasource

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kintmin.data.local.dto.YoutubeDownloadDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class PythonExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun downloadYoutubeMedia(
        youtubeUrl: String,
        audioDownloadPath: String,
    ) = runCatching {
        withContext(Dispatchers.IO) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            withTimeout(TIME_OUT) {
                val module = Python.getInstance().getModule(PYTHON_FILE_NAME)
                val pyResult =
                    module.callAttr(PYTHON_METHOD_NAME, youtubeUrl, audioDownloadPath).asList()

                if (pyResult.size == 1) {
                    throw Exception(pyResult[0].toString())
                } else {
                    YoutubeDownloadDto(
                        title = pyResult[0].toString(),
                        thumbnailDownloadUrl = pyResult[1].toString(),
                    )
                }
            }
        }
    }

    companion object {
        const val TIME_OUT = 30000L
        const val PYTHON_FILE_NAME = "download_youtube_audio"
        const val PYTHON_METHOD_NAME = "download_audio"
    }
}

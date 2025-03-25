package com.kintmin.pythonbridge

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kintmin.pythonbridge.model.YoutubeDownloadDto
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
                val module = Python.getInstance().getModule(PYTHON_FILE_NAME)
                val pyResult =
                    module.callAttr(PYTHON_METHOD_NAME, youtubeUrl, audioDownloadPath).asList()

                if (pyResult.size == 1) {
                    Log.d("EXCEPTION", pyResult[0].toString())
                    throw Exception(pyResult[0].toString())
                } else {
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
    }

    companion object {
        const val TIME_OUT = 60000L
        const val PYTHON_FILE_NAME = "download_youtube_audio"
        const val PYTHON_METHOD_NAME = "download_audio"
    }
}

package com.kintmin.data.python_bridge

import com.kintmin.data.python_bridge.model.YoutubeDownloadDto

interface PythonExecutor {
    suspend fun downloadYoutubeMedia(
        youtubeUrl: String,
        audioDownloadPath: String,
    ): Result<YoutubeDownloadDto>
}
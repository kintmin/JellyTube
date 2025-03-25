package com.kintmin.pythonbridge

import com.kintmin.pythonbridge.model.YoutubeDownloadDto

interface PythonExecutor {
    suspend fun downloadYoutubeMedia(
        youtubeUrl: String,
        audioDownloadPath: String,
    ): Result<YoutubeDownloadDto>
}
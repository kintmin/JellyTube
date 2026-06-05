package com.kintmin.data.python_bridge

import com.kintmin.data.python_bridge.model.YoutubeDownloadDto

interface IosPythonExecutorBridge {
    suspend fun downloadYoutubeMedia(
        youtubeUrl: String,
        audioDownloadPath: String,
    ): YoutubeDownloadDto

    suspend fun extractYoutubeUrlsFromPlaylist(
        playlistUrl: String,
    ): List<String>
}

internal class IosPythonExecutorAdapter(
    private val bridge: IosPythonExecutorBridge,
) : PythonExecutor {

    override suspend fun downloadYoutubeMedia(
        youtubeUrl: String,
        audioDownloadPath: String,
    ): Result<YoutubeDownloadDto> = runCatching {
        bridge.downloadYoutubeMedia(
            youtubeUrl = youtubeUrl,
            audioDownloadPath = audioDownloadPath,
        )
    }

    override suspend fun extractYoutubeUrlsFromPlaylist(playlistUrl: String): Result<List<String>> = runCatching {
        bridge.extractYoutubeUrlsFromPlaylist(playlistUrl)
    }
}

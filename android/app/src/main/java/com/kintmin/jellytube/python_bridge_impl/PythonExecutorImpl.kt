package com.kintmin.jellytube.python_bridge_impl

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.python_bridge.model.PlayableAudioDownloadDto
import com.kintmin.data.python_bridge.model.YoutubeDownloadDto
import com.kintmin.data.python_bridge.model.withDefaults
import com.kintmin.domain.common.suspendRunCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PythonExecutorImpl(
    private val context: Context
) : PythonExecutor {

    override suspend fun downloadYoutubeMedia(
        youtubeUrl: String,
        audioDownloadPath: String,
    ) = suspendRunCatching {
        withContext(Dispatchers.IO) {
            ensurePythonStarted()

            val module = Python.getInstance().getModule(FILE_NAME)
            val version = module.callAttr(METHOD_GET_VERSION).toString()
            Log.d("DEBUG", "yt-dlp version: $version")

            val pyResult =
                module.callAttr(METHOD_DOWNLOAD_AUDIO, youtubeUrl, audioDownloadPath).asList()

            YoutubeDownloadDto(
                title = pyResult.getOrNull(0)?.toString().orEmpty(),
                thumbnailDownloadUrl = pyResult.getOrNull(1)?.toString().orEmpty(),
                duration = pyResult.getOrNull(2)?.toString().orEmpty(),
                uploader = pyResult.getOrNull(3)?.toString().orEmpty(),
                description = pyResult.getOrNull(4)?.toString().orEmpty(),
                audioFileNameWithExt = pyResult.getOrNull(5)?.toString().orEmpty(),
            ).withDefaults()
        }
    }

    override suspend fun downloadPlayableAudio(
        youtubeUrl: String,
        audioDownloadPath: String,
    ): Result<PlayableAudioDownloadDto> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            ensurePythonStarted()

            val module = Python.getInstance().getModule(FILE_NAME)
            val pyResult = module.callAttr(METHOD_DOWNLOAD_PLAYABLE_AUDIO, youtubeUrl, audioDownloadPath).asList()
            PlayableAudioDownloadDto(
                audioFileNameWithExt = pyResult.getOrNull(0)?.toString().orEmpty(),
                duration = pyResult.getOrNull(1)?.toString().orEmpty(),
            )
        }
    }

    override suspend fun extractYoutubePlaylist(playlistUrl: String): Result<String> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            ensurePythonStarted()

            val module = Python.getInstance().getModule(FILE_NAME)
            module.callAttr(METHOD_EXTRACT_PLAYLIST, playlistUrl).asList().first().toString()
        }
    }

    // 동시에 처음 불리면 둘 다 시작하려 들고 Chaquopy 는 두 번째 시작에서 던진다.
    private fun ensurePythonStarted() = synchronized(PythonExecutorImpl::class) {
        if (!Python.isStarted()) Python.start(AndroidPlatform(context))
    }

    private companion object {
        const val FILE_NAME = "download_youtube_audio"
        const val METHOD_GET_VERSION = "get_version"
        const val METHOD_DOWNLOAD_AUDIO = "download_audio"
        const val METHOD_DOWNLOAD_PLAYABLE_AUDIO = "download_playable_audio"
        const val METHOD_EXTRACT_PLAYLIST = "extract_playlist"
    }
}

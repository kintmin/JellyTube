package com.kintmin.data.python_bridge

import com.kintmin.data.python_bridge.model.PlayableAudioDownloadDto
import com.kintmin.data.python_bridge.model.YoutubeDownloadDto

interface PythonExecutor {
    suspend fun downloadYoutubeMedia(
        youtubeUrl: String,
        audioDownloadPath: String,
    ): Result<YoutubeDownloadDto>

    /**
     * 이 기기의 플레이어가 여는 형식으로 음원 파일만 받는다. 앱 프로세스 안에서 받으므로 앱이 떠 있는
     * 동안 써야 한다. 저장된 확장자가 내용과 다를 수 있다(HLS → ADTS 가 `.mp4`).
     */
    suspend fun downloadPlayableAudio(
        youtubeUrl: String,
        audioDownloadPath: String,
    ): Result<PlayableAudioDownloadDto>

    suspend fun extractYoutubeUrlsFromPlaylist(
        playlistUrl: String,
    ): Result<List<String>>
}
package com.kintmin.data.python_bridge

import com.kintmin.data.python_bridge.model.PlayableAudioDownloadDto
import com.kintmin.data.python_bridge.model.YoutubeDownloadDto
import com.kintmin.data.python_bridge.model.withDefaults

interface IosPythonExecutorBridge {
    suspend fun downloadYoutubeMedia(
        youtubeUrl: String,
        audioDownloadPath: String,
    ): YoutubeDownloadDto

    /** `download_youtube_audio.py` 의 `download_playable_audio`. 앱 프로세스 안에서 받는다. */
    suspend fun downloadPlayableAudio(
        youtubeUrl: String,
        audioDownloadPath: String,
    ): PlayableAudioDownloadDto

    suspend fun extractYoutubeUrlsFromPlaylist(
        playlistUrl: String,
    ): List<String>

    /** `transliterate_lyrics.py` 의 `transliterate_to_korean`. 줄 구조를 보존한다. */
    suspend fun transliterateToKorean(text: String): String

    /**
     * 앱이 내려가 있는 사이에 끝난 전송의 원본 주소.
     *
     * 전송은 OS 데몬이 하므로 앱이 없는 동안에도 끝난다. 그때는 등록해 줄 코루틴이 없어
     * 파일만 자리에 놓여 있는데, 앱이 다시 뜰 때 이 목록으로 같은 주소를 한 번 더 태우면
     * 전송 없이 등록까지 이어진다.
     */
    fun takeFinishedDownloadUrls(): List<String>
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
        ).withDefaults()
    }

    override suspend fun downloadPlayableAudio(
        youtubeUrl: String,
        audioDownloadPath: String,
    ): Result<PlayableAudioDownloadDto> = runCatching {
        bridge.downloadPlayableAudio(youtubeUrl = youtubeUrl, audioDownloadPath = audioDownloadPath)
    }

    override suspend fun extractYoutubeUrlsFromPlaylist(playlistUrl: String): Result<List<String>> = runCatching {
        bridge.extractYoutubeUrlsFromPlaylist(playlistUrl)
    }
}

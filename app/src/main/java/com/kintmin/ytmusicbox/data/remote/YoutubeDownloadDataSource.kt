package com.kintmin.ytmusicbox.data.remote

import com.ar.youtubeextractor.core.YouTubeExtractor
import com.ar.youtubeextractor.core.onError
import com.ar.youtubeextractor.core.onSuccess
import com.ar.youtubeextractor.model.VideoDetails
import com.kintmin.ytmusicbox.data.remote.api.YoutubeDownloadApi
import com.kintmin.ytmusicbox.data.remote.dto.YoutubeDownloadDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody
import javax.inject.Inject

class YoutubeDownloadDataSource @Inject constructor(
    private val api: YoutubeDownloadApi,
) {

    private val youTubeExtractor by lazy { YouTubeExtractor() }

    suspend fun download(
        youtubeUrl: String,
    ) = runCatching {
        withContext(Dispatchers.IO) {
            withTimeout(10000L) {
                var videoData: VideoDetails? = null
                var downloadUrl = ""

                youTubeExtractor.extractVideoData(youtubeUrl).onSuccess {
                    videoData = it.videoDetails
                    downloadUrl =
                        it.streamingData.hlsManifestUrl ?: throw Exception("다운 url을 찾을 수 없습니다.")
                }.onError { error ->
                    throw Exception(error.toString())
                }

                val response = api.downloadYoutubeM3U8(downloadUrl)
                if (!response.isSuccessful) {
                    throw Exception("HTTP 오류: ${response.code()} - ${response.errorBody()}")
                }

                val body: ResponseBody = response.body() ?: throw Exception("응답 본문이 없습니다.")

                YoutubeDownloadDto(
                    title = videoData?.title,
                    description = videoData?.shortDescription,
                    rawData = body.bytes(),
                )
            }
        }
    }
}

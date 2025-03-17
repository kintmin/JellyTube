package com.kintmin.ytmusicbox.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface YoutubeDownloadApi {
    @Streaming
    @GET
    suspend fun downloadYoutubeM3U8(@Url url: String): Response<ResponseBody>
}
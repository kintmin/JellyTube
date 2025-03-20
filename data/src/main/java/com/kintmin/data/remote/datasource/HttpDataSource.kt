package com.kintmin.data.remote.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class HttpDataSource @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun downloadImage(imageUrl: String): Result<ByteArray> = runCatching {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(imageUrl)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception(response.body.toString())
            }

            response.body!!.bytes()
        }
    }
}
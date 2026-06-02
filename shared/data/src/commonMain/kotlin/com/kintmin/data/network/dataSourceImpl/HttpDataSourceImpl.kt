package com.kintmin.data.network.dataSourceImpl

import com.kintmin.data.network.dataSource.HttpDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class HttpDataSourceImpl constructor(
    private val client: OkHttpClient,
) : HttpDataSource {

    override suspend fun downloadImage(imageUrl: String): Result<ByteArray> = runCatching {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(imageUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("http error ${response.code}: ${response.message}")
                }
                response.body.bytes()
            }
        }
    }
}
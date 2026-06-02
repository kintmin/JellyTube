package com.kintmin.data.network.dataSourceImpl

import com.kintmin.data.network.dataSource.HttpDataSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class HttpDataSourceImpl constructor(
    private val client: HttpClient,
) : HttpDataSource {

    override suspend fun downloadImage(imageUrl: String): Result<ByteArray> = runCatching {
        client.get(imageUrl).body()
    }
}

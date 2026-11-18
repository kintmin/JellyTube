package com.kintmin.data.network.data_source

interface HttpDataSource {
    suspend fun downloadImage(imageUrl: String): Result<ByteArray>
}
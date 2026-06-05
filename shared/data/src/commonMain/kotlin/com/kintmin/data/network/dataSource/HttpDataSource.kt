package com.kintmin.data.network.dataSource

interface HttpDataSource {
    suspend fun downloadImage(imageUrl: String): Result<ByteArray>
}
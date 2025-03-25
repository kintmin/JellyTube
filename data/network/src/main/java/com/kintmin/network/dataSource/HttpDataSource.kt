package com.kintmin.network.dataSource

interface HttpDataSource {
    suspend fun downloadImage(imageUrl: String): Result<ByteArray>
}
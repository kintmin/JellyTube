package com.kintmin.data.network.dataSource

import com.kintmin.fileshare.MigrationManifestDto

interface MigrationDataSource {

    /** 거부되면(401/404) null. */
    suspend fun pair(host: String, port: Int, code: String): Result<String?>

    suspend fun fetchManifest(host: String, port: Int, token: String): Result<MigrationManifestDto>

    /** 본문을 [onChunk] 로 흘려보낸다. 파일 하나를 메모리에 다 올리지 않는다. */
    suspend fun downloadFile(
        host: String,
        port: Int,
        token: String,
        kind: String,
        name: String,
        onChunk: (ByteArray, Int) -> Unit,
    ): Result<Unit>
}

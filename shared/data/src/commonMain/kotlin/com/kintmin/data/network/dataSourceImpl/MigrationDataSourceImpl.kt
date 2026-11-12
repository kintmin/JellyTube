package com.kintmin.data.network.dataSourceImpl

import com.kintmin.data.data_migration.localDeviceName
import com.kintmin.data.network.dataSource.MigrationDataSource
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.MigrationFinishRequest
import com.kintmin.fileshare.MigrationManifestDto
import com.kintmin.fileshare.MigrationPairRequest
import com.kintmin.fileshare.MigrationPairResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.utils.io.readAvailable

internal class MigrationDataSourceImpl(
    private val client: HttpClient,
) : MigrationDataSource {

    override suspend fun pair(host: String, port: Int, code: String): Result<String> = runCatching {
        client.post {
            migrationUrl(host, port, FileShareConstants.MIGRATION_PAIR_PATH)
            contentType(ContentType.Application.Json)
            setBody(MigrationPairRequest(code = code, deviceName = localDeviceName()))
            expectSuccess = true
        }.body<MigrationPairResponse>().token
    }

    override suspend fun fetchManifest(host: String, port: Int, token: String): Result<MigrationManifestDto> =
        runCatching {
            client.get {
                migrationUrl(host, port, FileShareConstants.MIGRATION_MANIFEST_PATH)
                bearerAuth(token)
                expectSuccess = true
                // 곡이 많으면 보내는 쪽이 파일 크기를 재는 데 시간이 걸린다.
                timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
            }.body()
        }

    override suspend fun finish(
        host: String,
        port: Int,
        token: String,
        newAudioMediaCount: Int,
        failedCount: Int,
    ): Result<Unit> = runCatching {
        client.post {
            migrationUrl(host, port, FileShareConstants.MIGRATION_FINISH_PATH)
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(MigrationFinishRequest(newAudioMediaCount, failedCount))
            expectSuccess = true
        }
        Unit
    }

    override suspend fun downloadFile(
        host: String,
        port: Int,
        token: String,
        kind: String,
        name: String,
        onChunk: (ByteArray, Int) -> Unit,
    ): Result<Unit> = runCatching {
        client.prepareGet {
            migrationUrl(host, port, FileShareConstants.MIGRATION_FILES_PATH)
            url.appendPathSegments(kind, name)
            bearerAuth(token)
            expectSuccess = true
            // 큰 파일은 공용 클라이언트의 요청 시간 상한을 넘긴다. 멈춤은 소켓 시간 상한이 잡는다.
            timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
        }.execute { response ->
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = channel.readAvailable(buffer, 0, buffer.size)
                if (read == -1) break
                if (read > 0) onChunk(buffer, read)
            }
        }
    }

    private fun HttpRequestBuilder.migrationUrl(host: String, port: Int, path: String) {
        url {
            protocol = URLProtocol.HTTP
            this.host = host
            this.port = port
            encodedPath = path
        }
    }
}

private const val BUFFER_SIZE = 64 * 1024
